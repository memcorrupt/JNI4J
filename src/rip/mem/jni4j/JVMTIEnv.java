package rip.mem.jni4j;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;

import rip.mem.jni4j.JNIFunctions.JNIFunctionDef;

public class JVMTIEnv {

	private static JVMTIEnv instance;

	private final MemorySegment pointer;
	private final JNIFunctions functions;

	private static final JNIFunctionDef[] functionDefs = new JNIFunctionDef[] {
			new JNIFunctionDef(
					JNIConstants.JVMTI_FUNCTION_STOPTHREAD,
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					),

			new JNIFunctionDef(
					JNIConstants.JVMTI_FUNCTION_ADDCAPABILITIES,
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					)
	};

	private static final Arena arena = Arena.ofAuto();
	private static final Linker linker = Linker.nativeLinker();

	synchronized static JVMTIEnv getInstance() throws Throwable {
		//return cached jvmtiEnv
		if(instance != null)
			return instance;

		var envPointer = JavaVM.getInstance().getEnv(JNIConstants.JVMTI_VERSION_19);

		return (instance = new JVMTIEnv(envPointer));
	}

	private JVMTIEnv(MemorySegment pointer) {
		this.pointer = pointer;
		this.functions = new JNIFunctions(linker, pointer, JNIConstants.JNI_NATIVE_FUNCTION_COUNT, functionDefs);
	}

	static void stopThread(MemorySegment klass, MemorySegment exception) throws Throwable {
		getInstance()._stopThread(klass, exception);
	}

	private void _stopThread(MemorySegment klass, MemorySegment exception) throws Throwable {
		var res = (int) functions
				.getFunction(JNIConstants.JVMTI_FUNCTION_STOPTHREAD)
				.invokeExact(pointer, klass, exception);

		checkForError("StopThread", res);
	}

	static void addCapabilities(JVMTICapabilities capabilities) throws Throwable {
		getInstance()._addCapabilities(capabilities);
	}

	private void _addCapabilities(JVMTICapabilities capabilities) throws Throwable {
		var capsBuf = capabilities.serialize();
		var capsPtr = arena.allocate(capsBuf.length);
		MemorySegment.copy(capsBuf, 0, capsPtr, ValueLayout.JAVA_BYTE, 0, capsBuf.length);
		
		var res = (int) functions
				.getFunction(JNIConstants.JVMTI_FUNCTION_ADDCAPABILITIES)
				.invokeExact(pointer, capsPtr);

		checkForError("AddCapabilities", res);
	}

	private void checkForError(String method, int result) {
		var error = JNIConstants.getJVMTIErrorName(result);

		if(JNI4J.DEBUG)
			System.out.println(String.format("jvmtiEnv->%s status: %s", method, error));

		if(result != JNIConstants.JVMTI_ERROR_NONE)
			throw new RuntimeException(String.format("jvmtiEnv->%s failed: %s", method, error));
	}

	public static class JVMTICapabilities {

		public boolean canTagObjects;
		public boolean canGenerateFieldModificationEvents;
		public boolean canGenerateFieldAccessEvents;
		public boolean canGetBytecodes;
		public boolean canGetSyntheticAttribute;
		public boolean canGetOwnedMonitorInfo;
		public boolean canGetCurrentContendedMonitor;
		public boolean canGetMonitorInfo;
		public boolean canPopFrame;
		public boolean canRedefineClasses;
		public boolean canSignalThread;
		public boolean canGetSourceFileName;
		public boolean canGetLineNumbers;
		public boolean canGetSourceDebugExtension;
		public boolean canAccessLocalVariables;
		public boolean canMaintainOriginalMethodOrder;
		public boolean canGenerateSingleStepEvents;
		public boolean canGenerateExceptionEvents;
		public boolean canGenerateFramePopEvents;
		public boolean canGenerateBreakpointEvents;
		public boolean canSuspend;
		public boolean canRedefineAnyClass;
		public boolean canGetCurrentThreadCpuTime;
		public boolean canGetThreadCpuTime;
		public boolean canGenerateMethodEntryEvents;
		public boolean canGenerateMethodExitEvents;
		public boolean canGenerateAllClassHookEvents;
		public boolean canGenerateCompiledMethodLoadEvents;
		public boolean canGenerateMonitorEvents;
		public boolean canGenerateVmObjectAllocEvents;
		public boolean canGenerateNativeMethodBindEvents;
		public boolean canGenerateGarbageCollecitonEvents;
		public boolean canGenerateObjectFreeEvents;
		public boolean canForceEarlyReturn;
		public boolean canGetOwnedMonitorStackDepthInfo;
		public boolean canGetConstantPool;
		public boolean canSetNativeMethodPrefix;
		public boolean canRetransformClasses;
		public boolean canRetransformAnyClass;
		public boolean canGenerateResourceExhaustionHeapEvents;
		public boolean canGenerateResourceExhaustionThreadsEvents;
		public boolean canGenerateEarlyVmstart;
		public boolean canGenerateEarlyClassHookEvents;
		public boolean canGenerateSampledObjectAllocEvents;
		public boolean canSupportVirtualThreads;

		public byte[] serialize() {
			byte[] buf = new byte[16];
			int idx = 0;
			int bit = 0;

			try {
				for(Field f : getClass().getFields()) {
					buf[idx] |= (f.getBoolean(this) ? 1 : 0) << bit++;

					if(bit >= 8) {
						bit = 0;
						idx++;
					}
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("Cannot serialize JVMTICapabilities", e);
			}

			return buf;
		}

		public static JVMTICapabilities deserialize(byte[] buf) {
			if(buf.length != 16)
				throw new IllegalArgumentException("JVMTICapabilities must be a 16-byte packed struct");

			var c = new JVMTICapabilities();
			int idx = 0;
			int bit = 0;

			try {
				for(var f : c.getClass().getFields()) {
					f.setBoolean(c, ((buf[idx] >> bit++) & 1) != 0);

					if(bit >= 8) {
						bit = 0;
						idx++;
					}
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("Cannot serialize JVMTICapabilities", e);
			}

			return c;
		}

		@Override
		public String toString() {
			var fields = getClass().getFields();
			var values = new ArrayList<String>();

			try {
				for(var f : fields) {
					values.add(f.getName() + "=" + f.getBoolean(this));
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("Cannot generate toString for JVMTICapabilities", e);
			}

			return "JVMTICapabilities [" + String.join(", ", values) + "]";
		}

	}

}
