/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.windup.ui.internal.editor;

import static org.jboss.tools.windup.ui.WindupUIPlugin.IMG_HINT;
import static org.jboss.tools.windup.ui.WindupUIPlugin.IMG_JAVA;
import static org.jboss.tools.windup.ui.WindupUIPlugin.IMG_RULE;
import static org.jboss.tools.windup.ui.WindupUIPlugin.IMG_XML_RULE;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.misc.StringMatcher;
import org.eclipse.ui.internal.misc.StringMatcher.Position;
import org.jboss.tools.windup.ui.WindupUIPlugin;
import org.jboss.tools.windup.ui.internal.editor.RulesetElementUiDelegateFactory.IElementUiDelegate;
import org.jboss.tools.windup.ui.internal.editor.RulesetElementUiDelegateFactory.RulesetConstants;
import org.jboss.tools.windup.ui.internal.explorer.IssueExplorer;
import org.jboss.tools.windup.ui.internal.rules.xml.XMLRulesetModelUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.Lists;

@SuppressWarnings("restriction")
@Creatable
public class RulesSectionContentProvider extends StyledCellLabelProvider implements ITreeContentProvider, ILabelProvider, IStyledLabelProvider {
	
	private static Color YELLOW = Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
	
	private static final Image RULE;
	private static final Image XML_NODE;
	private static final Image JAVA;
	private static final Image HINT;

	static {
		ImageRegistry imageRegistry = WindupUIPlugin.getDefault().getImageRegistry();
		RULE = imageRegistry.get(IMG_RULE);
		XML_NODE = imageRegistry.get(IMG_XML_RULE);
		JAVA = imageRegistry.get(IMG_JAVA);
		HINT = imageRegistry.get(IMG_HINT);
	}
	
	private RulesetElementUiDelegateRegistry elementUiRegistry = new RulesetElementUiDelegateRegistry(new RulesetElementUiDelegateFactory()); 
	
	private Text filterText;
	
	public void setFilterText(Text filterText) {
		this.filterText = filterText;
	}
	
	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof Document) {
			Document document = (Document)element;
			List<Node> rules = Lists.newArrayList();
			XMLRulesetModelUtil.collectRuleNodes(document, rules);
			return rules.toArray();
		}
		
		Object[] children = getDelegate((Element)element).getChildren();
		return Arrays.stream(children).filter(o -> o instanceof Element).collect(Collectors.toList()).toArray();
	}
	
	@Override
	public Object getParent(Object element) {
		if (element instanceof Element) {
			return ((Element) element).getParentNode();
		}
		return null;
	}
	
	public boolean hasChildren(Object element) {
		if (element instanceof Node) {
			return getChildren(element).length > 0;
		}
		return false;
	}
	
	@Override
	public String getText(Object node) {
		String text = "";
		if (node instanceof Element) {
			Element element = (Element)node; 
			if (isRuleNode(element)) {
				text = XMLRulesetModelUtil.getRuleId((Node)element);
			}
			else {
				text = doGetText(node);
			}
		}
		return text;
	}
	
	private String doGetText(Object object) {
		String result = null;
		if (object instanceof Node) {
			Node node = (Node) object;
			switch (node.getNodeType()) {
				case Node.ELEMENT_NODE : {
					result = node.getNodeName();
					break;
				}
			}
		}
		result = TextProcessor.process(result);
		return result != null ? result : ""; //$NON-NLS-1$
	}
	
	private boolean isRuleNode(Element element) {
		return RulesetConstants.RULE_NAME.equals(element.getNodeName());
	}
	
	private boolean isJavaClassNode(Element element) {
		return RulesetConstants.JAVACLASS_NAME.equals(element.getNodeName());
	}
	
	private boolean isHintNode(Element element) {
		return RulesetConstants.HINT_NAME.equals(element.getNodeName());
	}
	
	@Override
	public Image getImage(Object node) {
		Image image = null;
		if (node instanceof Element) {
			Element element = (Element)node;
			if (isRuleNode(element)) {
				image = RULE;
			}
			else if (isJavaClassNode(element)) {
				return JAVA;
			}
			else if (isHintNode(element)) {
				return HINT;
			}
			else {
				image = XML_NODE;
			}
		}
		return image;
	}

	@Override
	public StyledString getStyledText(Object element) {
		String text = filterText.getText();
		StyledString style = new StyledString(getText(element));
		if (element instanceof Element && !text.isEmpty()) {
			StringMatcher matcher = IssueExplorer.getFilterMatcher(text);
			String label = style.getString();
			Position position = matcher.find(label, 0, label.length());
			if (position != null && (position.getEnd() - position.getStart()) > 0) {
				style.setStyle(position.getStart(), position.getEnd() - position.getStart(), new Styler() {
					@Override
					public void applyStyles(TextStyle textStyle) {
						textStyle.background = YELLOW;
					}
				});
			}
		}
		return style;
	}
	
	@Override
	public void addListener(ILabelProviderListener listener) {}
	
	@Override
	public void removeListener(ILabelProviderListener listener) {}
	
	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}
	
	@Override
	public void dispose() {
	}
	
	private IElementUiDelegate getDelegate(Element element) {
		IEclipseContext context = WindupUIPlugin.getDefault().getContext();
		context = context.createChild();
		context.set(Element.class, element);
		return elementUiRegistry.getOrCreateUiDelegate(element, context);
	}
}
