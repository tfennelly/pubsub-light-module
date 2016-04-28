/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.event;

import net.sf.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * {@link MessageBus} message instance.
 * <p>
 * Purposely chose a very simple {@link Properties} based extension for the message
 * type, so as to avoid marshal/unmarshal issues with more complex message type
 * (the {@link MessageBus} implementation could be distributed).
 * 
 * <h1>Event property namespaces</h1>
 * Event property names are opaque {@link String}s. Any {@link String} is valid, but
 * we do recommend using valid Java package identifier type names e.g. "a.b.c". 
 * This will help to avoid name collisions.
 * <p>
 * <strong>NOTE</strong> that the "jenkins" namespace prefix of reserved.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
abstract class Message extends Properties {

    public static final String CHANNEL_NAME_KEY = "jenkins.channel";
    public static final String EVENT_NAME_KEY = "jenkins.event";
    
    public static final String OBJECT_NAME_KEY = "jenkins.object.name";
    public static final String OBJECT_ID_KEY = "jenkins.object.id";
    public static final String OBJECT_URL_KEY = "jenkins.object.url";
    
    /**
     * Create a plain message instance.
     */
    Message() {
    }

    /**
     * Get the Jenkins domain model object name (full name) that this message instance is
     * associated with.
     *
     * @return The Jenkins domain model object name (full name) that this message instance is
     * associated with.
     * @see #OBJECT_NAME_KEY
     */
    @CheckForNull protected String getObjectName() {
        return getProperty(OBJECT_NAME_KEY);
    }

    /**
     * Get the Jenkins domain model object Id that this message instance is
     * associated with.
     *
     * @return The Jenkins domain model object Id that this message instance is
     * associated with, or {@code null} if no id was set on this message instance.
     * @see #OBJECT_ID_KEY
     */
    @CheckForNull protected String getObjectId() {
        return getProperty(OBJECT_ID_KEY);
    }

    /**
     * Fluent property setter.
     * <p>
     * Same as {@link #setProperty(String, String)}, but returns {@code this}.
     * Also checks for {@code null} name and value args.
     * 
     * @param name Property name.
     * @param value Property value.
     * @return {@code this} message instance.
     */
    public Message set(String name, String value) {
        if (name != null && value != null) {
            setProperty(name, value);
        }
        return this;
    }

    /**
     * Get the channel name for the message.
     * @return The channel name for the message, or {@code null} if none set.
     */
    public String getChannelName() {
        return getProperty(CHANNEL_NAME_KEY);
    }
    
    /**
     * Set the channel name for the message.
     * @param name The channel name for the message.
     */
    public Message setChannelName(String name) {
        set(CHANNEL_NAME_KEY, name);
        return this;
    }
    
    /**
     * Get the event name for the message.
     * @return The event name for the message, or {@code null} if none set.
     */
    public String getEventName() {
        return getProperty(EVENT_NAME_KEY);
    }
    
    /**
     * Set the event name for the message.
     * @param name The event name for the message.
     */
    public Message setEventName(String name) {
        set(EVENT_NAME_KEY, name);
        return this;
    }

    /**
     * Clone this {@link Message} instance.
     * <p>
     * Base implementation creates a {@link SimpleMessage} instance.
     * @return The clone.
     */
    public Message clone() {
        Message clone = new SimpleMessage();
        clone.putAll(this);
        return clone;
    }

    /**
     * Does this message contain all of the properties supplied in the properties
     * argument.
     * @param properties The properties to check for.
     * @return {@code true} if this message contain all of the properties supplied in the properties
     * argument, otherwise {@code false}.
     */
    public boolean containsAll(@Nonnull Properties properties) {
        if (!properties.isEmpty()) {
            Set<Map.Entry<Object, Object>> entries = properties.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                String name = entry.getKey().toString();

                String actualValue = getProperty(name);
                if (actualValue == null) {
                    return false;
                } else if (!actualValue.equals(entry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Write the message properties to JSON.
     * @return The message properties as a String.
     */
    public final @Nonnull String toJSON() {
        StringWriter writer = new StringWriter();
        try {
            toJSON(writer);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected IOException while writing to a StringWriter.", e);
        }
        return writer.toString();
    }

    /**
     * Write the message properties as JSON to a {@link Writer}. 
     * @param writer The {@link Writer} instance.
     * @throws IOException Error writing to the {@link Writer}.
     */
    public final void toJSON(@Nonnull Writer writer) throws IOException {
        JSONObject json = JSONObject.fromObject(this);
        json.write(writer);
        writer.flush();
    }

    /**
     * Same as {@link #toJSON()}.
     * @return The message properties as a JSON String.
     */
    @Override
    public final String toString() {
        return toJSON();
    }
}
