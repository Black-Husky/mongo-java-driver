/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb;

import org.bson.BSON;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonInt32;
import org.bson.BsonObjectId;
import org.bson.Transformer;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.junit.Test;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistryHelper.fromProviders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DBObjectCodecTest extends DatabaseTestCase {

    @Test
    public void testTransformers() {
        try {
            collection.save(new BasicDBObject("_id", 1).append("x", 1.1));
            assertEquals(Double.class, collection.findOne().get("x").getClass());

            BSON.addEncodingHook(Double.class, new Transformer() {
                public Object transform(final Object o) {
                    return o.toString();
                }
            });

            collection.save(new BasicDBObject("_id", 1).append("x", 1.1));
            assertEquals(String.class, collection.findOne().get("x").getClass());

            BSON.clearAllHooks();
            collection.save(new BasicDBObject("_id", 1).append("x", 1.1));
            assertEquals(Double.class, collection.findOne().get("x").getClass());

            BSON.addDecodingHook(Double.class, new Transformer() {
                public Object transform(final Object o) {
                    return o.toString();
                }
            });
            assertEquals(String.class, collection.findOne().get("x").getClass());
            BSON.clearAllHooks();
            assertEquals(Double.class, collection.findOne().get("x").getClass());
        } finally {
            BSON.clearAllHooks();
        }
    }

    @Test
    public void testDBListEncoding() {
        BasicDBList list = new BasicDBList();
        list.add(new BasicDBObject("a", 1).append("b", true));
        list.add(new BasicDBObject("c", "string").append("d", 0.1));
        collection.save(new BasicDBObject("l", list));
        assertEquals(list, collection.findOne().get("l"));
    }

    @Test
    public void shouldNotGenerateIdIfPresent() {
        DBObjectCodec dbObjectCodec = new DBObjectCodec(fromProviders(asList(new ValueCodecProvider(), new DBObjectCodecProvider())));
        DBObject document = new BasicDBObject("_id", 1);
        assertTrue(dbObjectCodec.documentHasId(document));
        document = dbObjectCodec.generateIdIfAbsentFromDocument(document);
        assertTrue(dbObjectCodec.documentHasId(document));
        assertEquals(new BsonInt32(1), dbObjectCodec.getDocumentId(document));
    }

    @Test
    public void shouldGenerateIdIfAbsent() {
        DBObjectCodec dbObjectCodec = new DBObjectCodec(fromProviders(asList(new ValueCodecProvider(), new DBObjectCodecProvider())));
        DBObject document = new BasicDBObject();
        assertFalse(dbObjectCodec.documentHasId(document));
        document = dbObjectCodec.generateIdIfAbsentFromDocument(document);
        assertTrue(dbObjectCodec.documentHasId(document));
        assertEquals(BsonObjectId.class, dbObjectCodec.getDocumentId(document).getClass());
    }

    @Test
    public void shouldRespectEncodeIdFirstPropertyInEncoderContext() {
        DBObjectCodec dbObjectCodec = new DBObjectCodec(fromProviders(asList(new ValueCodecProvider(), new DBObjectCodecProvider())));
        // given
        DBObject doc = new BasicDBObject("x", 2).append("_id", 2);

        // when
        BsonDocument encodedDocument = new BsonDocument();
        dbObjectCodec.encode(new BsonDocumentWriter(encodedDocument),
                             doc,
                             EncoderContext.builder().isEncodingCollectibleDocument(true).build());

        // then
        assertEquals(new ArrayList<String>(encodedDocument.keySet()), asList("_id", "x"));

        // when
        encodedDocument.clear();
        dbObjectCodec.encode(new BsonDocumentWriter(encodedDocument),
                             doc,
                             EncoderContext.builder().isEncodingCollectibleDocument(false).build());

        // then
        assertEquals(new ArrayList<String>(encodedDocument.keySet()), asList("x", "_id"));
    }
}
