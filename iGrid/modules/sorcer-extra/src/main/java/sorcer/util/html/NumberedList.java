/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.util.html;

public class NumberedList extends Container {
	public NumberedList() {
	}

	public NumberedList(int start) {
		startingNumber = start;
	}

	public NumberedList(Component c) {
		add(c);
	}

	public void print(java.io.PrintWriter pw) {
		pw.print("<ol");
		if (type != ' ')
			pw.print(" type=" + type);

		if (startingNumber > -1)
			pw.print(" start=" + startingNumber);

		pw.println(">");

		super.print(pw);
		pw.println("</ol>");
	}

	private char type = ' ';
	private int startingNumber = -1;
}
