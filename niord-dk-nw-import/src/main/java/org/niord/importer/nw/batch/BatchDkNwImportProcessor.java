/*
 * Copyright 2016 Danish Maritime Authority.
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
package org.niord.importer.nw.batch;

import org.niord.core.message.Message;
import org.niord.core.message.batch.BatchMessageImportProcessor;

import javax.inject.Named;

/**
 * Processes legacy NW messages
 */
@Named
public class BatchDkNwImportProcessor extends BatchMessageImportProcessor {


    /** {@inheritDoc} **/
    @Override
    public Object processItem(Object item) throws Exception {

        Message message = (Message)item;

        // First, check if there is an existing message with the same legacy id
        Message original = messageService.findByLegacyId(message.getLegacyId());

        if (original != null) {
            if (original.getStatus() == message.getStatus()) {
                getLog().info("Skipping unchanged legacy NW: " + message.getLegacyId());
                return null;
            }

            // Update the original
            original.setStatus(message.getStatus());
            original.setPublishDateFrom(message.getPublishDateFrom());
            original.setPublishDateTo(message.getPublishDateTo());
            original.setAutoTitle(message.isAutoTitle());
            // TODO ... determine which other fields to copy

            getLog().info("Processed existing legacy NW: " + message.getLegacyId());
            return original;

        } else {
            // We have a new message
            // Process related message base data
            message = processMessage(message);
            getLog().info("Processed new legacy NW: " + message.getLegacyId());

            return message;
        }
    }
}
