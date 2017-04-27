/*
 * Copyright 2017 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.niord.core.promulgation;

import org.niord.core.message.Message;
import org.niord.core.message.MessageService;
import org.niord.model.message.Status;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.List;

/**
 * Manages "navwarn" promulgations.
 * <p>
 * The "navwarn" promulgation service sends out mails whenever an associated navigational warning (NW)
 * is published, cancelled, plus a daily in-force list.
 * <p>
 * Unlike e.g. NAVTEX, Audio and Twitter promulgations, this service is not associated with message promulgation data.
 *
 */
@Singleton
@Startup
@Lock(LockType.READ)
@SuppressWarnings("unused")
public class NavWarnPromulgationService extends BasePromulgationService {

    public static final String SERVICE_ID = "navwarn";

    @Inject
    MessageService messageService;

    @Inject
    PromulgationTypeService promulgationTypeService;


    /***************************************/
    /** Promulgation Service Handling     **/
    /***************************************/


    /** {@inheritDoc} */
    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }


    /** {@inheritDoc} */
    @Override
    public String getServiceName() {
        return "Navwarn mailing list";
    }



    /***************************************/
    /** NavWarn promulgation              **/
    /***************************************/


    /**
     * Handle Navwarn promulgation for the message
     * @param messageUid the UID of the message
     */
    @Asynchronous
    public void checkPromulgateMessage(String messageUid) {

        Message message = messageService.findByUid(messageUid);

        if (message != null && (message.getStatus() == Status.PUBLISHED || message.getStatus() == Status.CANCELLED)) {
            List<PromulgationType> navWarnTypes = promulgationTypeService.getActivePromulgationTypes(SERVICE_ID, message);
            navWarnTypes.forEach(t -> promulgateMessage(message, t));
        }

    }


    /**
     * Handle Twitter promulgation for the message
     * @param message the message
     * @param type the Navwarn promulgation type
     */
    private void promulgateMessage(Message message, PromulgationType type) {

        long t0 = System.currentTimeMillis();

        log.info("XXXXXXXXXXXXX NAVWARN " + message.getId());
    }

}
