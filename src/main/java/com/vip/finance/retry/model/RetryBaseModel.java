package com.vip.finance.retry.model;

import java.io.Serializable;

public abstract class RetryBaseModel implements Serializable {

    public abstract void check();
}
