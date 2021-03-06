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
package org.jenkins.pubsub;

import hudson.model.Item;
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
 * {@link PubsubBus} message instance.
 * 
 * <h1>Light-weight messages</h1>
 * We purposely chose a very simple {@link Properties} based extension for the message
 * type, so as to avoid marshal/unmarshal issues with more complex message type
 * (the {@link PubsubBus} implementation could be distributed). It is also hoped that
 * this approach will promote the use of very light-weight event messages that contain
 * "just enough" information as to allow {@link ChannelSubscriber}s to decide if they
 * are interested in the event (or not). If they are interested in the event, they can
 * use standard Jenkins mechanisms to gain access to the full domain model object(s)
 * relating to the event.
 * <p>
 * <strong>Note</strong> that the use of lose typing is very intentional as complex types
 * are a notorious source of problems in distributed (the
 * {@link GuavaPubsubBus default bus implementation} is not distributed, but one could
 * implement one) asynchronous libraries. Also consider that this should not be a major
 * inconvenience if you stick with light-weight events i.e. sending complex/bloated events
 * is already considered as being an anti-pattern here.
 * <p>
 * <strong>Note</strong> the {@link AccessControlledMessage} subtype.
 *  
 * <h1>Event property namespaces</h1>
 * Event property names are opaque {@link String}s. Any {@link String} is valid, but
 * we do recommend using valid underscores to namespace e.g. "a_b_c". 
 * This will help to avoid name collisions.
 * <p>
 * <strong>NOTE</strong> that the "jenkins" namespace prefix of reserved e.g. "jenkins_channel".
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class Message<T extends Message> extends Properties {

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
     * @see EventProps.Jenkins#jenkins_object_name
     */
    @CheckForNull protected String getObjectName() {
        return get(EventProps.Jenkins.jenkins_object_name);
    }

    /**
     * Get the Jenkins domain model object Id that this message instance is
     * associated with.
     *
     * @return The Jenkins domain model object Id that this message instance is
     * associated with, or {@code null} if no id was set on this message instance.
     * @see EventProps.Jenkins#jenkins_object_id
     */
    @CheckForNull protected String getObjectId() {
        return get(EventProps.Jenkins.jenkins_object_id);
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
    public T set(String name, String value) {
        if (name != null && value != null) {
            setProperty(name, value);
        }
        return (T) this;
    }

    /**
     * Fluent property setter (by enum).
     * 
     * @param name Property name enum.
     * @param value Property value.
     * @return {@code this} message instance.
     */
    public T set(Enum name, String value) {
        return set(name.name(), value);
    }

    /**
     * Get the named message property value.
     * @param name Propery name.
     * @return The property value, or {@code null} if not defined.
     */
    public String get(String name) {
        return getProperty(name);
    }

    /**
     * Get the named message property value (by enum).
     * @param name Propery name enum.
     * @return The property value, or {@code null} if not defined.
     */
    public String get(Enum name) {
        return getProperty(name.name());
    }
    
    /**
     * Get the channel name for the message.
     * @return The channel name for the message, or {@code null} if none set.
     */
    public String getChannelName() {
        return get(EventProps.Jenkins.jenkins_channel);
    }
    
    /**
     * Set the channel name for the message.
     * @param name The channel name for the message.
     */
    public T setChannelName(String name) {
        set(EventProps.Jenkins.jenkins_channel, name);
        return (T) this;
    }
    
    /**
     * Get the event name for the message.
     * @return The event name for the message, or {@code null} if none set.
     */
    public String getEventName() {
        return get(EventProps.Jenkins.jenkins_event);
    }
    
    /**
     * Set the event name for the message.
     * @param name The event name for the message.
     */
    public T setEventName(String name) {
        set(EventProps.Jenkins.jenkins_event, name);
        return (T) this;
    }
    
    /**
     * Set the event name for the message.
     * @param name The event name for the message.
     */
    public T setEventName(Enum name) {
        set(EventProps.Jenkins.jenkins_event, name.name());
        return (T) this;
    }
    
    /**
     * Set {@link Item} propertis on the message instance.
     * @param item The Jenkins {@link Item}.
     */
    protected T setItemProps(@Nonnull Item item) {
        set(EventProps.Jenkins.jenkins_object_name, item.getFullName());
        set(EventProps.Jenkins.jenkins_object_url, item.getUrl());
        return (T) this;
    }
    
    // NOTE: Purposely not adding an Enum version of getEventName in 
    //       case a new event name (not defined in the enum) was used.

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
