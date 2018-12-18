## common-retry
基于resilience4j实现的代码级别retry，以及基于db存储+定时任务的retry补偿，可用于多种场景下的重试实现，并可自定义重试次数、衰减时间、重试调用的方法等

## common-retry 接入指南
common-retry组件并未上传到公开mvn仓库，可下载后通过 mvn clean deploy 命令上传到本地私服

## 接入步骤

### 步骤：

#### 1、在业务库中创建两张表，分别名为retry_info\retry_record，用于保存重试数据

##### 表结构：

**fund_retry_info**

```sql
CREATE TABLE "retry_info" (
  "id" int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  "biz_id" varchar(45) NOT NULL COMMENT '业务ID，对应的业务ID',
  "bean_class_name" varchar(128) NOT NULL,
  "bean_name" varchar(45) NOT NULL COMMENT '重试需要调用的方法所在的bean',
  "method_name" varchar(45) NOT NULL COMMENT '重试需要调用的方法',
  "retry_count" int(11) NOT NULL DEFAULT '0' COMMENT '已重试次数',
  "max_retry_count" int(11) NOT NULL COMMENT '最大重试次数',
  "status" varchar(20) NOT NULL COMMENT '当前状态\nRETRYING - 重试中\nRETRY_FAIL - 超出最大重试次数，不再重试\nFINISH - 重试成功，结束',
  "priority" int(11) DEFAULT '1' COMMENT '重试优先级，重试时会优先处理，目前暂时 3-最高，2 -中等 1 - 普通 这三个级别',
  "req_params" varchar(1024) NOT NULL COMMENT '重试时的请求参数',
  "req_params_class_name" varchar(128) NOT NULL,
  "decay_type" varchar(15) DEFAULT 'no' COMMENT '衰减类型 no - 不衰减  increase -- 递增衰减 average -- 间隔恒定',
  "decay_interval" int(11) DEFAULT '60' COMMENT '衰减间隔，单位为秒，在递增衰减和间隔恒定衰减时均有用，默认为60秒',
  "retry_time" timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '重试开始的时间，比如5分钟后',
  "create_time" timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '创建时间',
  "update_time" timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '修改时间',
  PRIMARY KEY ("id"),
  KEY "biz_idx" ("biz_id") USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='fund层重试信息保存，在retryable失败之后落库，然后通过定时任务继续发起重试';
```

**重试记录表**

```sql
CREATE TABLE "retry_record" (
  "id" int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  "biz_id" varchar(45) NOT NULL,
  "retry_info_id" int(11) NOT NULL COMMENT '关联的retry_info信息',
  "operate_type" varchar(45) NOT NULL,
  "reason" varchar(256) DEFAULT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  KEY "retry_info_idx" ("retry_info_id","biz_id")
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8 COMMENT='重试记录';
```

 

#### 2、引入依赖：

**dependencies**

```xml
# libraries.gradle:
gradle:  common_retry                : "com.retry.common:common-retry:0.0.1-SNAPSHOT"
 
# build.gradle 中需要引入common-retry依赖
compile(libraries.common_retry){
   exclude group: 'com.google.guava'
}
# 注意： 如果原系统中引入了guava，且版本低于26.0-jre，则需要在引入common-retry时排除guava
 
# 如果是maven，则引入(guava的排除一样要做)：
<dependency>
  <groupId>com.retry.common</groupId>
  <artifactId>common-retry</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```



#### 3、修改 applicationContext.xml

```xml
# 加入import
<import resource="classpath*:applicationContext-common-retry.xml"/>
 
# 增加repository.java文件的配置，在basePackage中加入retry相关路径，用逗号隔开
<property name="basePackage" value="XXX,com.retry.common.repository" />
```

 

#### 4、集成代码：

common-retry组件中，所有对外暴露的服务都在RetryFacade接口中定义。详情如下：

```java
/**
 * 用于retry时做各种重试实现的分发
 */
public interface RetryFacade {
 
    /**
     * 重试分发，会扫描数据，按照优先级及时间处理
     * @return
     */
    String retryDispatch();
 
    /**
     * 暂停指定的重试记录，如果已经是暂停状态，幂等
     * 如果已经是成功状态，不允许重试
     * @param pauseRetryModel
     * @return
     */
    RetryResult pauseRetry(PauseRetryModel pauseRetryModel);
 
    /**
     * 恢复指定的重试记录，如果不是暂停状态，则不允许resume
     * @param resumeRetryModel
     * @return
     */
    RetryResult resumeRetry(PauseRetryModel resumeRetryModel);
 
    /**
     * 根据id bizId status查询重试列表
     * @param queryRetryInfoModel
     * @return
     */
    RetryResult queryRetryListByParams(QueryRetryModel queryRetryInfoModel);
 
    /**
     * 根据id bizId 查询重试记录列表
     * @param queryRetryInfoModel
     * @return
     */
    RetryResult queryRetryRecordByParams(QueryRetryModel queryRetryInfoModel);
 
    /**
     * 保存重试数据
     * @param retryInfoModel
     * @return
     */
    RetryResult saveRetryInfo(RetryInfoModel retryInfoModel);
}
```

一些常量都定义在了common-retry中的enum类，比如是否需要衰减：

```java
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum DecayTypeEnum {
 
    NO("no"), // 不需要衰减
 
    INCREASE("increase"), // 递增衰减
 
    AVERAGE("average") // 间隔恒定
    ;
    private String type;
}
```

这里详细说明下衰减的时间计算规则，比如初始的重试开始时间为 2018-12-03 21:00:00

如果不需要衰减，那么此重试记录的执行时间为 2018-12-03 21:00:00  重试间隔根据对应的job定时任务间隔时间来确定。

如果是需要间隔恒定，这里允许传入每次间隔的时间，单位为秒，比如为60秒， 也就是每次重试失败后，下一次的重试时间需要加一分钟，假设2018-12-03 21:00:00马上执行且失败，则最新的重试开始时间为：2018-12-03 21:01:00。

如果是需要递增衰减，同样的允许传入每次间隔的时间间隔区间，单位为秒，比如为60秒，那么在重试失败一次之后，其最新的重试时间为：2018-12-03 21:01:00 。如果再执行一次失败，那么重试时间为： 2018-12-03 21:01:00 + 当前失败次数（2） *  时间间隔区间（60秒） = 2018-12-03 21:03:00 

#### 4.1 代码级别的重试

在使用定时任务重试之前，一般会先在代码中重试几次，毕竟定时任务还是有一定的延迟。

common-retry中基于resilience4j-retry封装了一个简单的代码重试方法，而不是简单粗暴的for循环，sleep重试实现方式，另外在重试失败之后，会自动将数据插入到retry_info表中，以备后续定时任务扫描处理。下面简单介绍下。

common-retry中提供了一个抽象类 AbstractRetryComponent<T,R>，其中T代表请求，R代表response响应，代码级别的重试需要继承这个抽象类，并实现其中的抽象方法。

```java
@Component
@Data
@Slf4j
@NoArgsConstructor
public abstract class AbstractRetryComponent<T, R> {
 
    @Autowired
    private RetryFacade retryFacade;
 
    /**
     * 模板类，用于实现retry及最终失败后的重试数据入库
     * @param t 请求
     * @return 处理结果
     */
    public R retryTemplate(T t) {
 
        log.info(" request:{}",
                JSONObject.toJSONString(t));
 
        RetryComponentConfig retryComponentConfig = initRetryConfig();
 
        RetryConfig retryConfig = RetryConfig.custom().maxAttempts(retryComponentConfig.getRetryTimes())
                .waitDuration(Duration.of(retryComponentConfig.getDuration(), retryComponentConfig.getDurationUnit()))
                .retryExceptions(CommonRetryException.class)
                .build();
 
        RetryRegistry registry = RetryRegistry.of(retryConfig);
 
        Retry retry = registry.retry("retry-template" + ThreadLocalRandom.current().nextLong());
 
 
        CheckedFunction1<T, R> decorated =
                Retry.decorateCheckedFunction(retry, t1 -> handleBiz(t1));
 
        R response = null;
        try {
            response = decorated.apply(t);
        } catch (CommonRetryException throwable) {
            log.info("after retry failed, go go go save retry info......");
            log.error("error:", throwable.getMessage());
            RetryInfoModel retryInfoModel = wrapRetryInfoInDb(t);
 
            retryInfoModel.check();
 
            retryFacade.saveRetryInfo(retryInfoModel);
        } catch (Throwable throwable) {
            log.error("error:", throwable.getMessage());
        }
 
        return response;
    }
 
    /**
     * 处理实际的retry操作，这里只需要调用super.retryTemplate(t) 即可。外层业务调用也直接调用当前bean的这个方法
     * 
     * 主要目的是指定T R 泛型对应的具体请求及响应类
     * @param t
     * @return
     */
    public abstract R handleBizWithRetry(T t);
 
    /**
     * 初始化retry相关配置，需要传入对应的重试次数、间隔时间
     *
     * @return
     */
    public abstract RetryComponentConfig initRetryConfig();
 
    /**
     * 具体的retry业务逻辑，各个业务方自己实现，定时任务job也要调用这个方法
     * 注意，如果有异常，一定是要封装成CommonRetryException抛出来，因为重试框架指定处理了这个异常。
     * @param t
     * @return
     * @throws CommonRetryException
     */
    public abstract R handleBiz(T t) throws CommonRetryException;
 
    /**
     * retry N次失败后，需要将对应数据存入db 用于后续的批处理重试，这里构造对应的数据
     *
     * @param t
     * @return
     */
    public abstract RetryInfoModel wrapRetryInfoInDb(T t);
 
}
```

可以看到，需要实现其中的四个抽象方法，具体的使用方法见上面注释。

看个实现类demo：

```java
@Slf4j
@Service(value = "creditQueryService")
public class CreditQueryServiceImpl extends AbstractRetryComponent<CreditResultQueryRequest, CreditResultQueryResponse> {
 
    @Override
    public CreditResultQueryResponse handleBizWithRetry(CreditResultQueryRequest creditResultQueryRequest) {
        return super.retryTemplate(creditResultQueryRequest);
    }
 
    @Override
    public RetryComponentConfig initRetryConfig() {
        return new RetryComponentConfig(3, 5, ChronoUnit.SECONDS);
    }
 
    // 处理业务逻辑
    @Override
    public CreditResultQueryResponse handleBiz(CreditResultQueryRequest request) throws CommonRetryException {
         
        // 触发重试
        if ("983345059".equals(request.getUserId())) {
            System.out.println("retry..........");
            throw new CommonRetryException("UserId Error");
        }
 
        System.out.println("invoke success");
        return new CreditResultQueryResponse();
 
    }
 
    @Override
    public RetryInfoModel wrapRetryInfoInDb(CreditResultQueryRequest creditResultQueryRequest) {
 
        RetryInfoModel retryInfoModel = new RetryInfoModel();
 
        retryInfoModel.setBeanClassName(this.getClass());
        retryInfoModel.setBeanName("hxbCreditServiceRetryService");
        retryInfoModel.setBizId(creditResultQueryRequest.getRequestNo());
        retryInfoModel.setDecayType(FalloffTypeEnum.NO.getType());
        retryInfoModel.setMaxRetryCount(99);
        retryInfoModel.setDecayInterval(30);
        retryInfoModel.setPriority(RetryPriorityEnum.LOW.getPriority());
        retryInfoModel.setMethodName("handleBiz");
        retryInfoModel.setReqParams(JSON.toJSONString(creditResultQueryRequest));
        retryInfoModel.setReqParamsClassName(CreditResultQueryRequest.class);
 
        return retryInfoModel;
    }
 
}
```

外面业务层调用，就直接使用  creditQueryService.handleBizWithRetry(T t) 即可。

#####  4.2 定时任务级别的重试

在4.1重试多次依然失败时，定时任务会作为补充继续重试下去，上面实现的wrapRetryInfoInDb方法就构造了一个重试需要设置的内容。

注意其中的请求数据：

```java
private String bizId; // 业务ID，必传
private Class beanClassName; // bean对应的class
private String beanName; // 重试时需要调用的beanName
private String methodName; // 重试时需要调用的methodName，如果是实现了上面的抽象类，则这里一定要传handleBiz，而不是handleBizWithRetry，避免有重复的重试数据入库。
private Integer priority; // 当前重试的优先级 不传的话默认是3 --普通
private String reqParams; // 请求参数，json格式
private Class reqParamsClassName; // 请求参数对应的class
private String decayType; // 超时时间类型，详见FalloffTypeEnum
private Integer decayInterval; // 延时时间，默认为60秒，单位为秒
private Integer maxRetryCount; // 最大重试次数，不传默认为99
private int retryTime; // 多长时间后重试，单位为秒,不传的话默认为30，即30秒之后重试
```

这里后续的重试发起，是采用java反射获取到对应的类、方法，并传入参数，所以对应的重试代码需要自己控制幂等、数据落地。

 

##### 4.3 系统需要定义自己的定时任务job，此job调用需要调用 retryFacade.retryDispatch()



#### 5 关于单元测试：

在单元测试的情况下，可能出现通过线程池发起的多线程代码未被调用的情况，可以参考这里： <https://blog.csdn.net/evan_leung/article/details/51835085>

简单来说，解决方案就是在test方法最后面，加入 System.in.read() 或者 Thread.sleep(5000) 来防止test 主线程退出过早，导致线程池还未被正常初始化。其中Thread.sleep()中指定的时间最好根据不同业务做相应评估