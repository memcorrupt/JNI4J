package rip.mem.jni4j;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class JavaVM {

	static {
		JNI4J.enableForeignAccess();
	}
	
	private static JavaVM instance;
	
	private final MemorySegment pointer; //JavaVM *
	private final MemorySegment functions; //JNIInvokeInterface_ *
	private static final MemoryLayout functionsLayout = MemoryLayout.sequenceLayout(JNIConstants.JNI_INVOKE_FUNCTION_COUNT, ValueLayout.ADDRESS);

	private static final FunctionDescriptor getEnv_signature = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT);
	private MethodHandle getEnv;

	private static final Arena arena = Arena.ofAuto();
	private static final Linker linker = Linker.nativeLinker();

	private JavaVM(MemorySegment pointer) {
		this.pointer = pointer;
		this.functions = pointer.get(ValueLayout.ADDRESS
				.withTargetLayout(functionsLayout), 0);
	}

	MemorySegment getEnv(int version) throws Throwable {
		if(getEnv == null) {
			var getEnv_ptr = functions.getAtIndex(ValueLayout.ADDRESS, JNIConstants.JNI_INVOKE_FUNCTION_GETENV);
			getEnv = linker.downcallHandle(getEnv_ptr, getEnv_signature);

			if(JNI4J.DEBUG)
				System.out.println(String.format("javaVM->GetEnv = 0x%x;", getEnv_ptr.address()));
		}

		var envOutput = arena.allocate(ValueLayout.ADDRESS);
		var res = (int) getEnv.invokeExact(pointer, envOutput, version);
		var error = JNIConstants.getJNIErrorName(res);
		
		if(JNI4J.DEBUG)
			System.out.println(String.format("javaVM->GetEnv status: %s", error));
		
		if(res != JNIConstants.JNI_OK)
			throw new RuntimeException(String.format("javaVM->GetEnv failed: %s", error));
		
		//(JNIEnv *) OR (jvmtiEnv *)
		//both contain a functions array as the first member
		var env = envOutput.get(ValueLayout.ADDRESS
				.withTargetLayout(ValueLayout.ADDRESS), 0);
		
		if(JNI4J.DEBUG)
			System.out.println(String.format("javaVM->GetEnv(%s) = 0x%x", JNIConstants.getEnvVersionName(version), env.address()));
		
		return env;
	}

	synchronized static JavaVM getInstance() throws Throwable {
		//return cached VM if exists
		if(instance != null)
			return instance;

		var jvmLib = SymbolLookup.libraryLookup("jvm", arena);

		var getCreatedVMs_address = jvmLib.find("JNI_GetCreatedJavaVMs").get();
		var getCreatedVMs_signature = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
		var getCreatedVMs = linker.downcallHandle(getCreatedVMs_address, getCreatedVMs_signature);

		var vmOutput = arena.allocateArray(ValueLayout.ADDRESS, 2);
		var vmCountPtr = arena.allocate(ValueLayout.JAVA_INT);
		var res = (int) getCreatedVMs.invokeExact(vmOutput, 2, vmCountPtr);

		var vmCount = vmCountPtr.get(ValueLayout.JAVA_INT, 0);
		var error = JNIConstants.getJNIErrorName(res);

		if(JNI4J.DEBUG)
			System.out.println(String.format("JNI_GetCreatedJavaVMs status: %s [returned %d VM(s)]", error, vmCount));

		if(res != JNIConstants.JNI_OK)
			throw new RuntimeException(String.format("JNI_GetCreatedJavaVMs failed: %s", error));

		if(vmCount != 1)
			throw new RuntimeException(String.format("JNI_GetCreatedJavaVMs returned an incorrect amount of VMs: expected 1, got %d", vmCount));

		var vmPointer = vmOutput.get(ValueLayout.ADDRESS
				.withTargetLayout(ValueLayout.ADDRESS), 0);

		if(JNI4J.DEBUG)
			System.out.println(String.format("JavaVM *javaVM = 0x%x;", vmPointer.address()));

		return (instance = new JavaVM(vmPointer));
	}

}
