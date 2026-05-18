package com.admissionmanagement.domain.application;

import java.time.LocalDateTime;

public class CommunicationEvent extends ApplicationEvent {
    private final CommunicationChannel channel;
    private final CommunicationResult result;
    private final String comment;

    public CommunicationEvent(
            CommunicationChannel channel,
            CommunicationResult result,
            String comment,
            LocalDateTime occurredAt
    ) {
        super(occurredAt);
        this.channel = channel;
        this.result = result;
        this.comment = comment;
    }

    public CommunicationChannel getChannel() {
        return channel;
    }

    public CommunicationResult getResult() {
        return result;
    }

    public String getComment() {
        return comment;
    }
}
