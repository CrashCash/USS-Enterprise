package org.genecash.enterprise;

import androidx.annotation.NonNull;

public class Command {
    private String bytes;
    private String prompt;
    private Integer id;

    public Command(String bytes, String prompt, Integer id) {
        this.bytes = bytes;
        this.prompt = prompt;
        this.id = id;
    }

    @NonNull
    @Override
    public String toString() {
        return this.prompt + " (" + this.bytes + ")";
    }

    public String getBytes() {
        return bytes;
    }

    public void setBytes(String bytes) {
        this.bytes = bytes;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
