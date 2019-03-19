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

package won.protocol.util;

import java.util.Iterator;

/**
 * Iterator that converts from type F (from) to type T (to).
 */
public abstract class ProjectingIterator<F, T> implements Iterator<T> {
    protected Iterator<F> baseIterator;

    protected ProjectingIterator(final Iterator<F> baseIterator) {
        this.baseIterator = baseIterator;
    }

    @Override
    public boolean hasNext() {
        return baseIterator.hasNext();
    }

    @Override
    public void remove() {
        baseIterator.remove();
    }
}
