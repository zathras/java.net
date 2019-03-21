/*
 * Task.java
 *
 * Copyright (C) 2005-2007 Tommi Laukkanen
 * http://www.substanceofcode.com
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

package com.substanceofcode.tasks;

/**
 *
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public abstract class AbstractTask implements Runnable {
    
    private Thread executionThread;
        
    public void execute() {
        executionThread = new Thread(this);
        executionThread.start();
    }

    public void run() {
        doTask();
    }
    
    public abstract void doTask();
        
}
