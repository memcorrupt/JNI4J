package rip.mem.jni4j;

import rip.mem.jni4j.JVMTIEnv.JVMTICapabilities;

public class JNI4JTest {

	public static void main(String[] args) {
		try {
			var c = new JVMTICapabilities();
			c.canSignalThread = true;
			
			JVMTIEnv.addCapabilities(c);
			
			//JVMTIEnv.stopThread(JNIEnv.toJNIHandle(Thread.currentThread()).getHandle(), JNIEnv.toJNIHandle(new ThreadDeath()).getHandle());
			System.out.println("DONE!");
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
