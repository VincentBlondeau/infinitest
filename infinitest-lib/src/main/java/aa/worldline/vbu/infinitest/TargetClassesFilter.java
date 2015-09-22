package aa.worldline.vbu.infinitest;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.io.filefilter.IOFileFilter;

public class TargetClassesFilter implements IOFileFilter, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean accept(File file) {
		// TODO Auto-generated method stub
		return file.isDirectory();
		
	}

	@Override
	public boolean accept(File dir, String name) {
		// TODO Auto-generated method stub
		return false;
	}

}
