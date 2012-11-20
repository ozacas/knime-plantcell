// AlignmentParseException.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.alignment;


/**
 * Exception thrown by ReadAlignment
 *
 * @author Korbinian Strimmer
 */
public class AlignmentParseException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4470067734479771531L;

	public AlignmentParseException() {}

	public AlignmentParseException(String msg)
	{
		super(msg);
	}
}
