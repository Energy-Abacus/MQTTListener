package org.abacus.script;

public class Topic {
    private String topic;
    private String name;
    private String idSeparator;
    private int idPosition;

    public Topic(String topic, String name, String idSeparator) {
        this.topic = topic;
        this.name = name;
        this.idSeparator = idSeparator;
        this.idPosition = getIdPosition(topic, idSeparator);
    }

    public String getTopic() {
        return topic;
    }

    public String getName() { return name; }

    public String getIdSeparator() {
        return idSeparator;
    }

    public int getIdPosition() {
        return idPosition;
    }

    private static int getIdPosition(String topic, String topicSeparator){
        String[] topicParts = topic.split(topicSeparator);

        for (int i = 0; i < topicParts.length; i++) {
            if(topicParts[i].equals("{id}")){
                return i;
            }
        }
        return -1;
    }

    public String getId() {
        String[] topicParts = topic.split(idSeparator);
        return topicParts[idPosition];
    }
}
