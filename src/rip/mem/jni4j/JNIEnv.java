package rip.mem.jni4j;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import rip.mem.jni4j.JNIFunctions.JNIFunctionDef;

public class JNIEnv {

	private static final ThreadLocal<JNIEnv> instance = new ThreadLocal<>();
	private static boolean NATIVES_REGISTERED = false;

	private final MemorySegment pointer; //JavaVM *
	private final JNIFunctions functions;

	private static final JNIFunctionDef[] functionDefs = new JNIFunctionDef[] {
			new JNIFunctionDef(
					JNIConstants.JNI_NATIVE_FUNCTION_FINDCLASS,
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					),

			new JNIFunctionDef(
					JNIConstants.JNI_NATIVE_FUNCTION_NEWGLOBALREF,
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					),

			new JNIFunctionDef(
					JNIConstants.JNI_NATIVE_FUNCTION_DELETEGLOBALREF,
					FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					),

			new JNIFunctionDef(
					JNIConstants.JNI_NATIVE_FUNCTION_GETMETHODID,
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					),

			new JNIFunctionDef(
					JNIConstants.JNI_NATIVE_FUNCTION_CALLOBJECTMETHODA,
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					),

			new JNIFunctionDef(
					JNIConstants.JNI_NATIVE_FUNCTION_GETSTATICMETHODID,
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					),

			new JNIFunctionDef(
					JNIConstants.JNI_NATIVE_FUNCTION_CALLSTATICOBJECTMETHODA,
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					),

			new JNIFunctionDef(
					JNIConstants.JNI_NATIVE_FUNCTION_NEWSTRINGUTF,
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
					),

			new JNIFunctionDef(
					JNIConstants.JNI_NATIVE_FUNCTION_REGISTERNATIVES,
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
					)
	};

	private static final Arena arena = Arena.ofAuto();
	private static final Linker linker = Linker.nativeLinker();
	private static final MemoryLayout jvalueLayout = MemoryLayout.unionLayout(
			ValueLayout.JAVA_BOOLEAN,
			ValueLayout.JAVA_BYTE,
			ValueLayout.JAVA_CHAR,
			ValueLayout.JAVA_SHORT,
			ValueLayout.JAVA_INT,
			ValueLayout.JAVA_LONG,
			ValueLayout.JAVA_FLOAT,
			ValueLayout.JAVA_DOUBLE,
			ValueLayout.ADDRESS
			);
	private static final long addressSize = ValueLayout.ADDRESS.byteSize();


	private static JNIEnv getInstance() throws Throwable {
		var inst = instance.get();

		//return cached instance if exists
		if(inst != null)
			return inst;

		instance.set(
				(inst = new JNIEnv(JavaVM.getInstance().getEnv(JNIConstants.JNI_VERSION_19)))
				);

		return inst;
	}

	private JNIEnv(MemorySegment pointer) {
		this.pointer = pointer;
		
		if(pointer.byteSize() < addressSize)
			pointer = pointer.reinterpret(addressSize);
		
		this.functions = new JNIFunctions(linker, pointer, JNIConstants.JNI_NATIVE_FUNCTION_COUNT, functionDefs);
	}

	private static MemorySegment findClass(String name) throws Throwable {
		return getInstance()._findClass(name);
	}

	private MemorySegment _findClass(String name) throws Throwable {
		var namePtr = arena.allocateUtf8String(name);

		return (MemorySegment) functions
				.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_NEWGLOBALREF)
				.invokeExact(pointer, 
						(MemorySegment) functions
						.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_FINDCLASS)
						.invokeExact(pointer, namePtr)
						);
	}

	private static MemorySegment newGlobalRef(MemorySegment ref) throws Throwable {
		return getInstance()._newGlobalRef(ref);
	}

	private MemorySegment _newGlobalRef(MemorySegment ref) throws Throwable {
		return (MemorySegment) functions
				.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_NEWGLOBALREF)
				.invokeExact(pointer, ref);
	}

	private static void deleteGlobalRef(MemorySegment ref) throws Throwable {
		getInstance()._deleteGlobalRef(ref);
	}

	private void _deleteGlobalRef(MemorySegment ref) throws Throwable {
		functions
		.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_DELETEGLOBALREF)
		.invokeExact(pointer, ref);
	}

	private static MemorySegment getMethodId(MemorySegment klass, String name, String signature) throws Throwable {
		return getInstance()._getMethodId(klass, name, signature);
	}

	private MemorySegment _getMethodId(MemorySegment klass, String name, String signature) throws Throwable {
		var namePtr = arena.allocateUtf8String(name);
		var sigPtr = arena.allocateUtf8String(signature);

		return (MemorySegment) functions
				.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_GETMETHODID)
				.invokeExact(pointer, klass, namePtr, sigPtr);
	}

	private static MemorySegment callObjectMethod(MemorySegment object, MemorySegment methodId, Object... args) throws Throwable {
		return getInstance()._callObjectMethod(object, methodId, args);
	}

	private MemorySegment _callObjectMethod(MemorySegment object, MemorySegment methodId, Object... args) throws Throwable {
		return (MemorySegment) functions
				.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_NEWGLOBALREF)
				.invokeExact(pointer,
						(MemorySegment) functions
						.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_CALLOBJECTMETHODA)
						.invokeExact(pointer, object, methodId, createJavaArgs(args))
						);
	}

	private static MemorySegment getStaticMethodId(MemorySegment klass, String name, String signature) throws Throwable {
		return getInstance()._getStaticMethodId(klass, name, signature);
	}

	private MemorySegment _getStaticMethodId(MemorySegment klass, String name, String signature) throws Throwable {
		var namePtr = arena.allocateUtf8String(name);
		var sigPtr = arena.allocateUtf8String(signature);

		return (MemorySegment) functions
				.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_GETSTATICMETHODID)
				.invokeExact(pointer, klass, namePtr, sigPtr);
	}

	private static MemorySegment callStaticObjectMethod(MemorySegment klass, MemorySegment methodId, Object... args) throws Throwable {
		return getInstance()._callStaticObjectMethod(klass, methodId, args);
	}

	private MemorySegment _callStaticObjectMethod(MemorySegment klass, MemorySegment methodId, Object... args) throws Throwable {
		return (MemorySegment) functions
				.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_NEWGLOBALREF)
				.invokeExact(pointer,
						(MemorySegment) functions
						.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_CALLSTATICOBJECTMETHODA)
						.invokeExact(pointer, klass, methodId, createJavaArgs(args))
						);
	}

	private static MemorySegment newStringUtf(String str) throws Throwable {
		return getInstance()._newStringUtf(str);
	}

	private MemorySegment _newStringUtf(String str) throws Throwable {
		var strPtr = arena.allocateUtf8String(str);

		return (MemorySegment) functions
				.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_NEWGLOBALREF)
				.invokeExact(pointer,
						(MemorySegment) functions
						.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_NEWSTRINGUTF)
						.invokeExact(pointer, strPtr)
						);
	}

	private static void registerNatives(MemorySegment klass, JNINativeMethod... methods) throws Throwable {
		getInstance()._registerNatives(klass, methods);
	}

	private void _registerNatives(MemorySegment klass, JNINativeMethod... methods) throws Throwable {
		var methodCount = methods.length;
		var nativeMethodInfo = arena.allocateArray(ValueLayout.ADDRESS, methodCount * 3);
		for(int i = 0; i < methods.length; i++) {
			int idx = i * 3;

			var method = methods[i];
			var namePtr = arena.allocateUtf8String(method.name);
			var sigPtr = arena.allocateUtf8String(method.signature);

			nativeMethodInfo.setAtIndex(ValueLayout.ADDRESS, idx, namePtr);
			nativeMethodInfo.setAtIndex(ValueLayout.ADDRESS, idx + 1, sigPtr);
			nativeMethodInfo.setAtIndex(ValueLayout.ADDRESS, idx + 2, method.handler);
		}

		var res = (int) functions
				.getFunction(JNIConstants.JNI_NATIVE_FUNCTION_REGISTERNATIVES)
				.invokeExact(pointer, klass, nativeMethodInfo, methodCount);

		if(res != JNIConstants.JNI_OK)
			throw new RuntimeException(String.format("jniEnv->RegisterNatives failed: %s", JNIConstants.getJNIErrorName(res)));
	}

	private MemorySegment createJavaArgs(Object... args) {
		var argsPtr = MemorySegment.ofAddress(0);
		var argsLen = args.length;
		if(argsLen > 0) {
			argsPtr = arena.allocateArray(jvalueLayout, argsLen);

			for(int i = 0; i < argsLen; i++) {
				long offset = jvalueLayout.byteSize() * i;

				switch(args[i]) {
				case MemorySegment val -> argsPtr.set(ValueLayout.ADDRESS, offset, val);
				case Boolean val -> argsPtr.set(ValueLayout.JAVA_BOOLEAN, offset, val);
				case Byte val -> argsPtr.set(ValueLayout.JAVA_BYTE, offset, val);
				case Character val -> argsPtr.set(ValueLayout.JAVA_CHAR, offset, val);
				case Double val -> argsPtr.set(ValueLayout.JAVA_DOUBLE, offset, val);
				case Float val -> argsPtr.set(ValueLayout.JAVA_FLOAT, offset, val);
				case Integer val -> argsPtr.set(ValueLayout.JAVA_INT, offset, val);
				case Long val -> argsPtr.set(ValueLayout.JAVA_LONG, offset, val);
				case Short val -> argsPtr.set(ValueLayout.JAVA_SHORT, offset, val);
				default -> throw new IllegalArgumentException(String.format("Received unsupported argument type in Call<type>Method: %s", args[i].getClass().getSimpleName()));
				}
			}
		}

		return argsPtr;
	}

	public static final class JNIRef implements AutoCloseable {

		private MemorySegment handle;

		private JNIRef(MemorySegment handle) {
			this.handle = handle;
		}

		public MemorySegment getHandle() {
			return handle;
		}

		@Override
		public void close() {
			try {
				deleteGlobalRef(handle);
			} catch (Throwable e) {
				System.out.println("JNI4J: failed to release global reference");
				e.printStackTrace();
			}
			handle = null;
		}

	}

	static JNIRef toJNIHandle(Object obj) throws Throwable {
		//TODO: cache handles in a weak table

		registerNatives();
		
		long addr = toJNIHandle_native(obj);
		
		var ex = nativeException.get();
		if(ex != null) {
			nativeException.remove();
			throw ex;
		}
		
		return new JNIRef(MemorySegment.ofAddress(addr));
	}

	private static native long toJNIHandle_native(Object obj);

	private static long toJNIHandle_handler(MemorySegment jniEnv, MemorySegment cls, MemorySegment obj) {
		var env = new JNIEnv(jniEnv);

		try {
			return env._newGlobalRef(obj).address();
		} catch (Throwable e) {
			nativeException.set(e);
			return 0;
		}
	}
	
	private static ThreadLocal<Throwable> nativeException = new ThreadLocal<>();

	//TODO: make this more automatic
	public static class JNINativeMethod {

		final String name;
		final String signature;
		final MemorySegment handler;

		JNINativeMethod(String name, String signature, MemorySegment handler){
			this.name = name;
			this.signature = signature;
			this.handler = handler;
		}

	}

	//TODO: rename or move to its own class
	private static void registerNatives() throws Throwable {
		if(NATIVES_REGISTERED)
			return;

		var threadKlass = findClass("java/lang/Thread");
		var classLoaderKlass = findClass("java/lang/ClassLoader");

		if(JNI4J.DEBUG) {
			System.out.println(String.format("Thread.class: 0x%x", threadKlass.address()));
			System.out.println(String.format("ClassLoader.class: 0x%x", classLoaderKlass.address()));
		}

		var currentThreadMethod = getStaticMethodId(threadKlass, "currentThread", "()Ljava/lang/Thread;");
		var getContextClassLoaderMethod = getMethodId(threadKlass, "getContextClassLoader", "()Ljava/lang/ClassLoader;");
		var loadClassMethod = getMethodId(classLoaderKlass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");

		if(JNI4J.DEBUG) {
			System.out.println(String.format("Thread#currentThread: 0x%x", currentThreadMethod.address()));
			System.out.println(String.format("Thread#getContextClassLoader: 0x%x", getContextClassLoaderMethod.address()));
			System.out.println(String.format("ClassLoader#loadClass: 0x%x", loadClassMethod.address()));
		}

		var klassName = newStringUtf(JNIEnv.class.getName());
		var currentThread = callStaticObjectMethod(threadKlass, currentThreadMethod);
		var classLoader = callObjectMethod(currentThread, getContextClassLoaderMethod);
		var thisKlass = callObjectMethod(classLoader, loadClassMethod, klassName);

		if(JNI4J.DEBUG) {
			System.out.println(String.format("Class Name: %s", klassName));
			System.out.println(String.format("Current Thread: 0x%x", currentThread.address()));
			System.out.println(String.format("Class Loader: 0x%x", classLoader.address()));
			System.out.println(String.format("This Class: 0x%x", thisKlass.address()));
		}

		registerNatives(thisKlass, new JNINativeMethod[] {
				new JNINativeMethod("toJNIHandle_native", "(Ljava/lang/Object;)J", linker.upcallStub(
						MethodHandles.lookup()
						.findStatic(
								JNIEnv.class,
								"toJNIHandle_handler", 
								MethodType.methodType(long.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)
								),
						FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS), 
						arena
						))
		});
		
		//cleanup
		deleteGlobalRef(threadKlass);
		deleteGlobalRef(classLoaderKlass);
		deleteGlobalRef(klassName);
		deleteGlobalRef(currentThread);
		deleteGlobalRef(classLoader);
		deleteGlobalRef(thisKlass);

		NATIVES_REGISTERED = true;
	}

}
