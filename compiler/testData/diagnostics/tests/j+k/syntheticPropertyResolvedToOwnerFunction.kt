// FILE: SomeJavaInterface.java

public interface SomeJavaInterface {
    Project getProject();
    void setProject(Project other);
}

// FILE: test.kt

class Project

class Outer(val project: Project) {
    val inner: SomeJavaInterface = object : SomeJavaInterface {
        override fun getProject(): Project? {
            project = null
            getProject()
            return <!SYNTHETIC_PROPERTY_RESOLVED_TO_OWNER!>project<!>
        }

        override fun setProject(other: Project?) {
            <!SYNTHETIC_PROPERTY_RESOLVED_TO_OWNER!>project<!> = other
            setProject(other)
            project
        }
    }
}