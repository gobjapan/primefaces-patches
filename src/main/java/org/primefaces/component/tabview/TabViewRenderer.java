/*
 * Copyright 2010 Prime Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.primefaces.component.tabview;

import java.io.IOException;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.StateManager;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.ServletResponse;

import org.primefaces.renderkit.CoreRenderer;
import org.primefaces.renderkit.PartialRenderer;
import org.primefaces.util.ComponentUtils;

public class TabViewRenderer extends CoreRenderer implements PartialRenderer {
	
	public void decode(FacesContext facesContext, UIComponent component) {
		Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
		TabView tabView = (TabView) component;
		String activeIndexValue = params.get(tabView.getClientId(facesContext) + "_activeIndex");
		
		if(!isValueEmpty(activeIndexValue)) {
			tabView.setActiveIndex(Integer.parseInt(activeIndexValue));
		}
	}

	public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
		TabView tabView = (TabView) component;
		
		encodeMarkup(facesContext, tabView);
		encodeScript(facesContext, tabView);
	}
	
	public void encodePartially(FacesContext facesContext, UIComponent component) throws IOException {
		TabView tabView = (TabView) component;

		Tab activeTab = null;

		int index = 0;
		for( UIComponent child : tabView.getChildren() ) {

			// ignore children that are not rendered
			if( !child.isRendered() ) {
				continue;
			}

			// is this the active tab?
			if( index == tabView.getActiveIndex() ) {
				activeTab = (Tab) child;
				break;
			}

			// proceed with next index
			index++;

		}
		
		ServletResponse response = (ServletResponse) facesContext.getExternalContext().getResponse();
		response.setContentType("text/xml");
		
		ResponseWriter writer = facesContext.getResponseWriter();
		try {
			writer.write("<?xml version=\"1.0\" encoding=\"" + response.getCharacterEncoding() + "\"?>");
			writer.write("<partialResponse>");
			
			//Tab content
			writer.write("<tabContent>");
			writer.startCDATA();
			renderChildren(facesContext, activeTab);
			writer.endCDATA();
			writer.write("</tabContent>");
	
			//State
			writer.write("<state>");
			writer.startCDATA();
			
			StateManager stateManager = facesContext.getApplication().getStateManager();
			stateManager.writeState(facesContext, stateManager.saveView(facesContext));
			
			writer.endCDATA();
			writer.write("</state>");
			
			writer.write("</partialResponse>");
		}catch(IOException exception) {
			exception.printStackTrace();
		}

		facesContext.responseComplete();
	}
	
	private void encodeScript(FacesContext facesContext, TabView tabView) throws IOException{
		ResponseWriter writer = facesContext.getResponseWriter();
		String clientId = tabView.getClientId(facesContext);
		String tabViewVar = createUniqueWidgetVar(facesContext, tabView);
		
		writer.startElement("script", null);
		writer.writeAttribute("type", "text/javascript", null);

	    writer.write(tabViewVar + " = new PrimeFaces.widget.TabView('" + clientId + "', {");
	    
	    writer.write("selected:" + tabView.getActiveIndex());
	    
	    if(tabView.isDynamic()) {
	    	UIComponent form = ComponentUtils.findParentForm(facesContext, tabView);
	    	if(form == null) {
		  	    throw new FacesException("TabView " + clientId + " must be nested inside a form when dynamic content loading is enabled");
	    	}
		  	    
	    	writer.write(",dynamic:true");
	    	writer.write(",url:'" + getActionURL(facesContext) + "'");
	    	writer.write(",formId:'" + form.getClientId(facesContext) + "'");
	  	    writer.write(",cache:" + tabView.isCache());
	  	    
	    } else {
	    	writer.write(",dynamic:false");
	    }
	    
	    if(tabView.isCollapsible()) writer.write(",collapsible:true");
	    if(tabView.getEvent() != null) writer.write(",event:'" + tabView.getEvent() + "'");
	    if(tabView.getEffect() != null) {
	    	writer.write(",fx: {");
	    	writer.write(tabView.getEffect() +":'toggle'");
	    	writer.write(",duration:'" + tabView.getEffectDuration() + "'");
	    	writer.write("}");
	    }
	    
	    writer.write("});");
	    
	    writer.endElement("script");
	}

	private void encodeMarkup(FacesContext facesContext, TabView tabView) throws IOException {
		ResponseWriter writer = facesContext.getResponseWriter();
		String clientId = tabView.getClientId(facesContext);
		int activeIndex = tabView.getActiveIndex();
		
		writer.startElement("div", tabView);
		writer.writeAttribute("id", clientId , null);
		
		encodeHeaders(facesContext, tabView, activeIndex);
		encodeContents(facesContext, tabView, activeIndex);
		
		encodeActiveIndexHolder(facesContext, tabView);
		
		writer.endElement("div");
	}

	private void encodeActiveIndexHolder(FacesContext facesContext, TabView tabView) throws IOException {
		ResponseWriter writer = facesContext.getResponseWriter();
		String paramName = tabView.getClientId(facesContext) + "_activeIndex";
		
		writer.startElement("input", null);
		writer.writeAttribute("type", "hidden", null);
		writer.writeAttribute("id", paramName, null);
		writer.writeAttribute("name", paramName, null);
		writer.writeAttribute("value", tabView.getActiveIndex(), null);
		writer.endElement("input");
	}
	
	private void encodeHeaders(FacesContext facesContext, TabView tabView, int activeTabIndex) throws IOException {
		ResponseWriter writer = facesContext.getResponseWriter();
		
		writer.startElement("ul", null);
		
		for(int i = 0; i < tabView.getChildren().size(); i++) {
			UIComponent kid = tabView.getChildren().get(i);
			
			if(kid.isRendered() && kid instanceof Tab) {
				Tab tab = (Tab) kid;
				
				writer.startElement("li", null);
				
				writer.startElement("a", null);
				writer.writeAttribute("href", "#" + tab.getClientId(facesContext), null);
				writer.startElement("em", null);
				writer.write(tab.getTitle());
				writer.endElement("em");
				writer.endElement("a");
				
				writer.endElement("li");
			}
		}
		
		writer.endElement("ul");
	}
	
	private void encodeContents(FacesContext facesContext, TabView tabView, int activeTabIndex) throws IOException {
		ResponseWriter writer = facesContext.getResponseWriter();
		
		// index of the tab ignoring tabs that are not rendered
		int visibleTabIndex = 0;
		
		for(int i = 0; i < tabView.getChildren().size(); i++) {
			UIComponent kid = tabView.getChildren().get(i);
			
			if(kid.isRendered() && kid instanceof Tab) {
				Tab tab = (Tab) kid;
				writer.startElement("div", null);
				writer.writeAttribute("id", tab.getClientId(facesContext), null);
				
				if(tabView.isDynamic()) {
					if(visibleTabIndex == activeTabIndex)
						renderChildren(facesContext, tab);
				} else {
					renderChildren(facesContext, tab);
				}
				
				writer.endElement("div");
				
				// proceed with the next tab
				visibleTabIndex++;
			}
		}
	}

	public void encodeChildren(FacesContext facesContext, UIComponent component) throws IOException {
		//Do nothing
	}

	public boolean getRendersChildren() {
		return true;
	}
}
