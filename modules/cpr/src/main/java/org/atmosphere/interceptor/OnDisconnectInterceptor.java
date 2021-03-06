/*
 * Copyright 2014 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.interceptor;

import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AsynchronousProcessor;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereInterceptorAdapter;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEventImpl;
import org.atmosphere.cpr.AtmosphereResourceFactory;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.HeaderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.atmosphere.cpr.FrameworkConfig.ASYNCHRONOUS_HOOK;

/**
 * When the browser close the connection, the atmosphere.js will send an unsubscribe message to tell
 * framework the browser is disconnecting.
 *
 * @author Jeanfrancois Arcand
 */
public class OnDisconnectInterceptor extends AtmosphereInterceptorAdapter {

    private final Logger logger = LoggerFactory.getLogger(OnDisconnectInterceptor.class);
    private AsynchronousProcessor p;

    @Override
    public void configure(AtmosphereConfig config) {
        if (AsynchronousProcessor.class.isAssignableFrom(config.framework().getAsyncSupport().getClass())) {
            p = AsynchronousProcessor.class.cast(config.framework().getAsyncSupport());
        }
    }

    @Override
    public Action inspect(final AtmosphereResource r) {
        AtmosphereRequest request = AtmosphereResourceImpl.class.cast(r).getRequest(false);
        String s = request.getHeader(HeaderConfig.X_ATMOSPHERE_TRANSPORT);
        String uuid = r.uuid();
        if (p != null && s != null && s.equalsIgnoreCase(HeaderConfig.DISCONNECT_TRANSPORT_MESSAGE)) {
            logger.trace("AtmosphereResource {} disconnected", uuid);
            AtmosphereResource ss = AtmosphereResourceFactory.getDefault().find(uuid);
            if (ss != null) {
                // Block websocket closing detection
                AtmosphereResourceImpl.class.cast(ss).getRequest(false).setAttribute(ASYNCHRONOUS_HOOK, null);
                AtmosphereResourceEventImpl.class.cast(ss.getAtmosphereResourceEvent()).isClosedByClient(true);

                p.completeLifecycle(ss, false);
            } else {
                logger.debug("Was unable to execute onDisconnect on {}", r.uuid());
            }
            return Action.CANCELLED;
        }
        return Action.CONTINUE;
    }

    public String toString() {
        return "Browser disconnection detection";
    }
}

