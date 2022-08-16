package be.ugent.knows.util;

import java.util.List;

public class MultipleAnswersException extends Exception{

    public MultipleAnswersException(int amountOfAnswers, List<String> answers){
        super("Expected to get a single answer but got "
                + amountOfAnswers
                + ": "
                + answers
        );
    }

}
