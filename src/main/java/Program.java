public class Program {
    private long id;
    private String className;
    private String sourceCode;
    private String sourceCodeTest;

    public Program(long id, String class_name, String sourceCode, String sourceCodeTest) {
        this.id = id;
        this.className = class_name;
        this.sourceCode = sourceCode;
        this.sourceCodeTest = sourceCodeTest;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getSourceCodeTest() {
        return sourceCodeTest;
    }

    public void setSourceCodeTest(String sourceCodeTest) {
        this.sourceCodeTest = sourceCodeTest;
    }
}
