/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.hyperaware.conference.eventmobi.http;

import com.hyperaware.conference.eventmobi.EventmobiApi;
import com.hyperaware.conference.eventmobi.EventmobiConfig;
import com.hyperaware.conference.mechanics.StreamingFetcher;
import com.hyperaware.conference.eventmobi.model.EmAgendaSectionResponse;
import com.hyperaware.conference.eventmobi.model.EmAttendeesSectionResponse;
import com.hyperaware.conference.eventmobi.model.EmCompaniesSectionResponse;
import com.hyperaware.conference.eventmobi.model.EmEventResponse;
import com.hyperaware.conference.eventmobi.model.EmMapsSectionResponse;
import com.hyperaware.conference.eventmobi.model.EmSection;
import com.hyperaware.conference.eventmobi.model.EmSpeakersSectionResponse;
import com.hyperaware.conference.eventmobi.parser.gson.GsonParser;
import com.hyperaware.conference.eventmobi.parser.gson.GsonSectionResponseParser;
import com.hyperaware.conference.mechanics.FetchException;
import com.hyperaware.conference.mechanics.Fetcher;
import com.hyperaware.conference.mechanics.Streamer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class HttpEventmobiApi implements EventmobiApi {

    private final EventmobiConfig config;
    // Map of section types to section data generated by
    // InterceptingEventFetcher after the EmEventResponse fetcher completes.
    private final HashMap<String, EmSection> sectionsByType = new HashMap<>();

    public HttpEventmobiApi(EventmobiConfig config) {
        this.config = config;
    }

    @Override
    public Fetcher<EmEventResponse> getEventFetcher() {
        final Streamer streamer = new HttpGetStreamer(config, config.getEventApiUrl() + ".json");
        return new InterceptingEventFetcher(streamer, new GsonParser<>(EmEventResponse.class));
    }

    @Override
    public Fetcher<EmAgendaSectionResponse> getAgendaSectionFetcher() {
        final Streamer streamer = new HttpGetStreamer(config, sectionsByType.get("agenda").getUrl() + ".json");
        return new StreamingFetcher<>(streamer, new GsonSectionResponseParser<>(EmAgendaSectionResponse.class));
    }

    @Override
    public Fetcher<EmSpeakersSectionResponse> getSpeakersSectionFetcher() {
        final Streamer streamer = new HttpGetStreamer(config, sectionsByType.get("speakers").getUrl() + ".json");
        return new StreamingFetcher<>(streamer, new GsonSectionResponseParser<>(EmSpeakersSectionResponse.class));
    }

    @Override
    public Fetcher<EmAttendeesSectionResponse> getAttendeesSectionFetcher() {
        final Streamer streamer = new HttpGetStreamer(config, sectionsByType.get("attendees").getUrl() + ".json");
        return new StreamingFetcher<>(streamer, new GsonSectionResponseParser<>(EmAttendeesSectionResponse.class));
    }

    @Override
    public Fetcher<EmMapsSectionResponse> getMapsSectionFetcher() {
        final Streamer streamer = new HttpGetStreamer(config, sectionsByType.get("maps").getUrl() + ".json");
        return new StreamingFetcher<>(streamer, new GsonSectionResponseParser<>(EmMapsSectionResponse.class));
    }

    @Override
    public Fetcher<EmCompaniesSectionResponse> getCompaniesSectionFetcher() {
        final Streamer streamer = new HttpGetStreamer(config, sectionsByType.get("companies").getUrl() + ".json");
        return new StreamingFetcher<>(streamer, new GsonSectionResponseParser<>(EmCompaniesSectionResponse.class));
    }


    /**
     * Special Fetcher that checks to see if the required sections are
     * available in the response before returning it.  It's then able
     * to build a map of section types to fetched section data for
     * fetching those sections later.
     */
    private class InterceptingEventFetcher extends StreamingFetcher<EmEventResponse> {
        public InterceptingEventFetcher(Streamer streamer, GsonParser<EmEventResponse> parser) {
            super(streamer, parser);
        }

        @Override
        public EmEventResponse fetch() throws FetchException {
            final EmEventResponse response = super.fetch();
            for (final EmSection section : response.getResponse().getSections()) {
                sectionsByType.put(section.getType(), section);
            }

            final List<String> required =
                Arrays.asList("agenda", "speakers", "companies", "attendees", "maps", "companies");
            for (final String type : required) {
                if (!sectionsByType.containsKey(type)) {
                    throw new FetchException(type + " section type not found in event response");
                }
            }

            return response;
        }
    }

}
