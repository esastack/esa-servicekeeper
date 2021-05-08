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
package esa.servicekeeper.core.retry.internal.impl;

import java.util.Map;

public class ExceptionCausePredicate extends ExceptionPredicateImpl {

    private static final int DEFAULT_CAUSE_EXAMINE_DEPTH = 8;

    private final boolean examineCauses;
    private final int maxCauseExamineDepth;

    public ExceptionCausePredicate(int maxAttempts,
                                   Map<Class<? extends Throwable>, Boolean> exceptions,
                                   Boolean defaultValue,
                                   boolean examineCauses, int maxCauseExamineDepth) {
        super(maxAttempts, exceptions, defaultValue);
        this.examineCauses = examineCauses;
        this.maxCauseExamineDepth = maxCauseExamineDepth > 0 ? maxCauseExamineDepth : DEFAULT_CAUSE_EXAMINE_DEPTH;
    }

    @Override
    protected boolean canRetry0(Throwable th) {
        boolean superValue = super.canRetry0(th);
        if (!examineCauses) {
            return superValue;
        }

        int examineCauseDepth = 0;
        if (superValue == (defaultValue)) {
            Throwable cause = th;
            do {
                if (this.exceptions.containsKey(cause.getClass())) {
                    return exceptions.get(cause.getClass());
                }
                cause = cause.getCause();
                superValue = super.canRetry0(cause);
                examineCauseDepth++;
            } while (examineCauseDepth <= maxCauseExamineDepth && cause != null && superValue == this.defaultValue);
        }

        return superValue;
    }

}
