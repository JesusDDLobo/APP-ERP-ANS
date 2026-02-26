package com.example.app_ans.tasks.network.dto;

import com.google.gson.annotations.SerializedName;

public class TaskCompletionAnswer {
    @SerializedName("question_id")
    private int questionId;
    @SerializedName("answer_text")
    private String answerText;
    @SerializedName("file_index")
    private Integer fileIndex;

    public TaskCompletionAnswer(int questionId, String answerText) {
        this.questionId = questionId;
        this.answerText = answerText;
    }

    public TaskCompletionAnswer(int questionId, String answerText, int fileIndex) {
        this.questionId = questionId;
        this.answerText = answerText;
        this.fileIndex = fileIndex;
    }
}
