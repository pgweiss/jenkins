/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Tom Huybrechts
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.util;

import hudson.ExtensionPoint;
import hudson.security.SecurityRealm;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * Servlet {@link Filter} that chains multiple {@link Filter}s, provided by plugins
 *
 * <p>
 * While this class by itself is not an extension point, I'm marking this class
 * as an extension point so that this class will be more discoverable.
 *
 * <p>
 * {@link SecurityRealm} that wants to contribute {@link Filter}s should first
 * check if {@link SecurityRealm#createFilter(FilterConfig)} is more appropriate.
 *
 * @see SecurityRealm
 */
public class PluginServletFilter implements Filter, ExtensionPoint {

	private static final List<Filter> LIST = new Vector<Filter>();
	
	private static FilterConfig filterConfig;
    
    public PluginServletFilter() {
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    	PluginServletFilter.filterConfig = filterConfig;
    	synchronized (LIST)  {
    		for (Filter f : LIST) {
    			f.init(filterConfig);
    		}
    	}
    }
    
    public static void addFilter(Filter filter) throws ServletException {
    	synchronized (LIST) {
    		if (filterConfig != null) {
    			filter.init(filterConfig);
    		}
    		LIST.add(filter);
    	}
    }

    public static void removeFilter(Filter filter) throws ServletException {
    	synchronized (LIST) {
            LIST.remove(filter);
    	}
    }

    public void doFilter(ServletRequest request, ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        new FilterChain() {
            private int position=0;
            // capture the array for thread-safety
            private final Filter[] filters = LIST.toArray(new Filter[LIST.size()]);

            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                if(position==filters.length) {
                    // reached to the end
                    chain.doFilter(request,response);
                } else {
                    // call next
                    filters[position++].doFilter(request,response,this);
                }
            }
        }.doFilter(request,response);
    }

    public void destroy() {
        synchronized (LIST)  {
            for (Filter f : LIST)
                f.destroy();
        }
    }
}
