package cz.knav.fedora.client;

import java.util.Calendar;

public class YearEvaluation {
    
    private int years;
        
    public YearEvaluation(int years) {
        super();
        this.years = years;
    }

    public boolean evaluate(int y) {
        int yNow = Calendar.getInstance().get(Calendar.YEAR);
        
        if (y < yNow - years) {
            return true;
        }
        return false;
    }

}
