package rip.mem.jni4j;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class JNI4J {

	public static boolean DEBUG = false;

	static {
		if(System.getenv("JNI4J_DEBUG") != null)
			DEBUG = true;
	}
	
	private static boolean FOREIGN_ACCESS_TRIED = false;

	@SuppressWarnings("preview")
	static void enableForeignAccess() {
		if(FOREIGN_ACCESS_TRIED)
			return;
		
		FOREIGN_ACCESS_TRIED = true;
		

		try {
			var module = JNI4J.class.getModule();
			if(module.isNativeAccessEnabled())
				return;
			
			
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Unsafe unsafe = (Unsafe) theUnsafe.get(null);

			var moduleKlass = module.getClass();

			Class<?> enableNativeAccessKlass = null;
			Class<?> archivedDataKlass = null;
			for(var candidate : moduleKlass.getDeclaredClasses()) {
				if(candidate.getSimpleName().equals("EnableNativeAccess")) {
					enableNativeAccessKlass = candidate;
				}

				if(candidate.getSimpleName().equals("ArchivedData")) {
					archivedDataKlass = candidate;
				}
			}

			if(enableNativeAccessKlass == null)
				throw new Exception("Cannot find Module$EnableNativeAccess class");

			if(!module.isNamed()) {
				if(archivedDataKlass == null)
					throw new Exception("Cannot find Module$ArchivedData class");
				
				var archiveInstance = archivedDataKlass.getDeclaredField("archivedData");
				
				@SuppressWarnings("deprecation")
				var archiveOffset = unsafe.staticFieldOffset(archiveInstance);
				var archiveData = unsafe.getObject(archivedDataKlass, archiveOffset);
				var unnamedModule = archivedDataKlass.getDeclaredField("allUnnamedModule");
				
				@SuppressWarnings("deprecation")
				var unnamedModuleOffset = unsafe.objectFieldOffset(unnamedModule);
				var allUnnamedModules = unsafe.getObject(archiveData, unnamedModuleOffset);
				
				module = (Module) allUnnamedModules;
			}

			Field targetField = enableNativeAccessKlass.getDeclaredField("FIELD_OFFSET");

			@SuppressWarnings("deprecation")
			var fieldOffset = unsafe.staticFieldOffset(targetField);
			var realOffset = unsafe.getLong(enableNativeAccessKlass, fieldOffset);

			unsafe.putBooleanVolatile(module, realOffset, true);
		}catch(Throwable e) {
			System.out.println("JNI4J: Unable to enable foreign access. Things may not work!");
			e.printStackTrace();
			return;
		}
	}

}
