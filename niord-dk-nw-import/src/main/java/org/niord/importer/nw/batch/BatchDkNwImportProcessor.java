/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
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
            original.setPublishDate(message.getPublishDate());
            original.setUnpublishDate(message.getUnpublishDate());
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
