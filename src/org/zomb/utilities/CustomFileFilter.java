package org.zomb.utilities;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class CustomFileFilter extends FileFilter implements java.io.FileFilter {
	private final String description;
	private final String[] extensions;

	public CustomFileFilter(String description, String... extensions) {
		this.description = description;
		this.extensions = extensions;

		for (int i = 0; i < extensions.length; i++) {
			extensions[i] = extensions[i].toLowerCase();
		}
	}

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;

		String s = f.getName();
		int i = s.lastIndexOf('.');
		String extension = i > 0 ? s.substring(i + 1).toLowerCase() : "";

		for (String wantedExt : extensions) {
			if (extension.equals(wantedExt)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String getDescription() {
		return description;
	}
}
