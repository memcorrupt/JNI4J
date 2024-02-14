package rip.mem.jni4j;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public class JNIFunctions {

	public static class JNIFunctionDef {
		
		private final int index;
		private final FunctionDescriptor signature;
		private MethodHandle method;
		
		JNIFunctionDef(int index, FunctionDescriptor signature){
			this.index = index;
			this.signature = signature;
		}
		
	}
	
	private final Linker linker;
	private final MemorySegment pointer;
	private final JNIFunctionDef[] functions;
	
	JNIFunctions(Linker linker, MemorySegment envPointer, int size, JNIFunctionDef... definitions) {
		this.linker = linker;
		
		//(JNINativeInterface_ *) OR (jvmtiInterface_1_ *)
		//both have function tables
		var functionsLayout = MemoryLayout.sequenceLayout(size, ValueLayout.ADDRESS);
		this.pointer = envPointer.get(ValueLayout.ADDRESS
				.withTargetLayout(functionsLayout), 0);
		
		if(JNI4J.DEBUG)
			System.out.println(String.format("0x%x->functions = 0x%x;", envPointer.address(), pointer.address()));
		
		this.functions = new JNIFunctionDef[size];
		for(var func : definitions) {
			if(func == null)
				continue;
			
			int index = func.index;
			
			if(index < 0 || index >= size)
				throw new IllegalArgumentException(String.format("Received JNIFunctionDef with invalid index: expected [0, %d], got %d", size, index));
		
			if(functions[index] != null)
				throw new IllegalArgumentException(String.format("Received JNIFunctionDef with duplicate index: %d", index));
			
			functions[index] = func;
		}
	}
	
	MethodHandle getFunction(int index){
		var func = functions[index];
		
		//return cached if exists
		if(func.method != null)
			return func.method;
		
		var address = pointer.getAtIndex(ValueLayout.ADDRESS, func.index);
		
		if(JNI4J.DEBUG)
			System.out.println(String.format("Resolved JNIFunctionDef 0x%x[%d]: 0x%x", pointer.address(), func.index, address.address()));
		
		return (func.method = linker.downcallHandle(address, func.signature));
	}
	
}
