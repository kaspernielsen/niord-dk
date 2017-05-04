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



var messageService = CdiUtils.getBean(org.niord.core.message.MessageService.class);

var date = java.util.Calendar.getInstance();
date.add(java.util.Calendar.DAY_OF_YEAR, 1);
params.put('firingExercisesDate', date.getTime());

var fromDate = org.niord.core.util.TimeUtils.resetTime(date.getTime());
var toDate = org.niord.core.util.TimeUtils.endOfDay(date.getTime());

var seriesIds = java.util.Collections.singleton('dma-fe');
var statuses = java.util.Collections.singleton(org.niord.model.message.Status.PUBLISHED);
var searchParams = new org.niord.core.message.MessageSearchParams();
searchParams.seriesIds(seriesIds)
    .statuses(statuses)
    .dateType(org.niord.core.message.MessageSearchParams$DateType.EVENT_DATE)
    .from(fromDate)
    .to(toDate);

var searchResult = messageService.search(searchParams);

var areaFilter = org.niord.model.DataFilter.get().fields('DETAILS').lang(language);

var result = new java.util.ArrayList();
for (var x = 0; x < searchResult.size; x++) {
    var fe = searchResult.data[x];
    var data = new java.util.HashMap();
    if (fe.areas === undefined || fe.areas.length === 0) {
        continue;
    }
    data.put('area', fe.areas[0].toVo(org.niord.model.message.AreaVo.class, areaFilter));

    var times = new java.util.ArrayList();
    for (var p = 0; p < fe.parts.length; p++) {
        var part = fe.parts[p];
        if (part.eventDates && part.eventDates.length > 0) {
            for (var d = 0; d < part.eventDates.length; d++) {
                var evtDate = part.eventDates[d];
                if (evtDate.fromDate !== undefined && evtDate.toDate !== undefined &&
                    evtDate.fromDate.before(toDate) &&
                    evtDate.toDate.after(fromDate)) {
                    times.add(evtDate.toVo());
                }
            }
        }
    }
    if (times.isEmpty()) {
        continue;
    }

    data.put('times', times);
    result.add(data);
}

params.put("firingExercises", result);

