public class Job {
    private String program;
    private long jobID;

    public Job(String programm, long jobID) {
        this.program = programm;
        this.jobID = jobID;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String programm) {
        this.program = programm;
    }

    public long getJobID() {
        return jobID;
    }

    public void setJobID(long jobID) {
        this.jobID = jobID;
    }
   
    
}
