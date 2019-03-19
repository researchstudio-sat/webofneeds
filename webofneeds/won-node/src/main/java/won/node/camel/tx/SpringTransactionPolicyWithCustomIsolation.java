/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.node.camel.tx;

import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SpringTransactionPolicy implementation that allows for setting the isolation behaviour name.
 */
public class SpringTransactionPolicyWithCustomIsolation extends SpringTransactionPolicy {

    private String isolationLevelName;

    public SpringTransactionPolicyWithCustomIsolation() {
    }

    public SpringTransactionPolicyWithCustomIsolation(TransactionTemplate template) {
        super(template);
    }

    public SpringTransactionPolicyWithCustomIsolation(PlatformTransactionManager transactionManager) {
        super(transactionManager);
    }

    public void setIsolationLevelName(String isolationLevelName) {
        this.isolationLevelName = isolationLevelName;
    }

    @Override
    public TransactionTemplate getTransactionTemplate() {
        TransactionTemplate answer = super.getTransactionTemplate();
        if (this.isolationLevelName != null) {
            answer.setIsolationLevelName(this.isolationLevelName);
        }
        return answer;
    }
}
