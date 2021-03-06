/*
 * Copyright 2021 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.esastack.servicekeeper.core.executionchain;

public class AsyncContext extends Context {

    private static final long serialVersionUID = -2340507448636679254L;

    /**
     * Saves the result of the original invocation. Note: This may be the result of fallback.
     */
    private transient volatile Object result;

    /**
     * Save the exception thrown by original invocation.
     */
    private volatile Throwable bizException;

    private volatile long spendTimeMs;

    public AsyncContext(String resourceId) {
        super(resourceId);
    }

    public AsyncContext(String resourceId, Object[] args) {
        super(resourceId, args);
    }

    @Override
    public Throwable getBizException() {
        return bizException;
    }

    @Override
    void setBizException0(Throwable bizException) {
        this.bizException = bizException;
    }

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    void setResult(Object result) {
        this.result = result;
    }

    @Override
    public long getSpendTimeMs() {
        return spendTimeMs;
    }

    @Override
    void setSpendTimeMs(long spendTimeMs) {
        this.spendTimeMs = spendTimeMs;
    }

}

