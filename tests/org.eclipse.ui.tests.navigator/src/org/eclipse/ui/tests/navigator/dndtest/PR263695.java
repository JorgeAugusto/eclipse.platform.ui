/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation

*******************************************************************************/
package org.eclipse.ui.tests.navigator.dndtest;

/*
 * Drag and Drop example snippet: drag text between two labels
 *
 * For a list of all SWT example snippets see
 * http://www.eclipse.org/swt/snippets/
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class PR263695 {

		public static void main(String[] args) {
				final Display display = new Display();

		Listener listener = event -> {
			switch (event.type) {
			case SWT.MouseDown:
				System.out.println("down");
				break;
			case SWT.MouseUp:
				System.out.println("up");
				break;
			case SWT.MouseMove:
				System.out.println("move");
				break;
			}
		};
				display.addFilter(SWT.MouseMove, listener);
				display.addFilter(SWT.MouseDown, listener);
				display.addFilter(SWT.MouseUp, listener);

				final Shell shell = new Shell(display);
				shell.setLayout(new FillLayout());
				final Label label1 = new Label(shell, SWT.BORDER);
				label1.setText("TEXT");
				final Label label2 = new Label(shell, SWT.BORDER);
				setDragDrop(label1);
				setDragDrop(label2);
				shell.setSize(200, 200);
				shell.open();

				Rectangle bounds = label1.getBounds();
				bounds = display.map(label1.getParent(), null, bounds);
				final int downX = bounds.x + (bounds.width / 2);
				final int downY = bounds.y + (bounds.height / 2);

				bounds = label2.getBounds();
				bounds = display.map(label2.getParent(), null, bounds);
				final int upX = bounds.x + (bounds.width / 2);
				final int upY = bounds.y + (bounds.height / 2);

				display.setCursorLocation(downX, downY);

		Thread t = new Thread(() -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
			}
			int sleep = 50;
			Event event = new Event();
			event.type = SWT.MouseDown;
			event.button = 1;
			display.post(event);

			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e2) {
			}
			event = new Event();
			event.type = SWT.MouseMove;
			event.x = downX;
			event.y = downY + 20;
			display.post(event);

			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e3) {
			}
			System.out.println("move to target");
			event = new Event();
			event.type = SWT.MouseMove;
			event.x = upX;
			event.y = upY;
			display.post(event);

			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e4) {
			}
			System.out.println("move inside target");
			event = new Event();
			event.type = SWT.MouseMove;
			event.x = upX;
			event.y = upY + 20;
			display.post(event);

			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e5) {
			}
			System.out.println("release");
			event = new Event();
			event.type = SWT.MouseUp;
			event.button = 1;
			display.post(event);
		});
				t.start();





				while (!shell.isDisposed()) {
						if (!display.readAndDispatch())
								display.sleep();
				}
				display.dispose();
		}

		public static void setDragDrop(final Label label) {

				Transfer[] types = new Transfer[] { TextTransfer.getInstance()
};
				int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

				final DragSource source = new DragSource(label, operations);
				source.setTransfer(types);
				source.addDragListener(new DragSourceListener() {
						@Override
						public void dragStart(DragSourceEvent event) {
								event.doit = (label.getText().length() != 0);
								System.out.println("dragStart: " + event);
						}

						@Override
						public void dragSetData(DragSourceEvent event) {
								event.data = label.getText();
						}

						@Override
						public void dragFinished(DragSourceEvent event) {
								if (event.detail == DND.DROP_MOVE)
										label.setText("");
						}
				});

				DropTarget target = new DropTarget(label, operations);
				target.setTransfer(types);
				target.addDropListener(new DropTargetAdapter() {
						@Override
						public void drop(DropTargetEvent event) {
								System.out.println("got event");
								if (event.data == null) {
										event.detail = DND.DROP_NONE;
										return;
								}
								label.setText((String) event.data);
						}
				});
		}
}

