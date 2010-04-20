package org.rosuda.mondrian.io;

public class ScanException extends Exception {

    public ScanException() {
    }


    public ScanException(String msg) {
        super(msg);
    }
}

/*
public class ScanException extends Exception {
	String scanerror = "";
	
	public ScanException(String error) {
		setExceptionMessage(error);
	}
	
	public String getExceptionMessage() {
		return scanerror;
	}
	public void setExceptionMessage(String error) {
		scanerror = error;
	}
}
*/