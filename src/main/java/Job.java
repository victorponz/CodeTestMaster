public class Job {
    private long  id;

    private String sourceCode;
    private String error;
    private int status;
    private Program program;

    public Job(Long id, String sourceCode, Program program) {
        this.id = id;
        this.sourceCode = sourceCode;
        this.program = program;
    }

    public synchronized long  getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }
}
