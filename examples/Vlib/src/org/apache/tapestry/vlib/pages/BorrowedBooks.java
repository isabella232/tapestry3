/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation", "Tapestry" 
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache" 
 *    or "Tapestry", nor may "Apache" or "Tapestry" appear in their 
 *    name, without prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE TAPESTRY CONTRIBUTOR COMMUNITY
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.tapestry.vlib.pages;

import java.rmi.RemoteException;

import javax.ejb.FinderException;

import org.apache.tapestry.ApplicationRuntimeException;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.vlib.Protected;
import org.apache.tapestry.vlib.VirtualLibraryEngine;
import org.apache.tapestry.vlib.Visit;
import org.apache.tapestry.vlib.components.Browser;
import org.apache.tapestry.vlib.ejb.Book;
import org.apache.tapestry.vlib.ejb.IBookQuery;
import org.apache.tapestry.vlib.ejb.IOperations;

/**
 *  Shows a list of the user's books, allowing books to be editted or
 *  even deleted.
 *
 *  <p>Note that, unlike elsewhere, book titles do not link to the
 *  {@link ViewBook} page.  It seems to me there would be a conflict between
 *  that behavior and the edit behavior; making the book titles not be links
 *  removes the ambiguity over what happens when the book title is clicked
 *  (view vs. edit).
 *
 *  @author Howard Lewis Ship
 *  @version $Id$
 * 
 **/

public class BorrowedBooks extends Protected
{
    private String message;
    private IBookQuery borrowedQuery;

    private Book currentBook;

    private Browser browser;

    public void detach()
    {
        message = null;
        borrowedQuery = null;
        currentBook = null;

        super.detach();
    }

    public void finishLoad()
    {
        browser = (Browser) getComponent("browser");
    }

    /**
     *  A dirty little secret of Tapestry and page recorders:  persistent
     *  properties must be set before the render (when this method is invoked)
     *  and can't change during the render.  We force
     *  the creation of the borrowed books query and re-execute it whenever
     *  the BorrowedBooks page is rendered.
     *
     **/

    public void beginResponse(IMarkupWriter writer, IRequestCycle cycle)
    {
        super.beginResponse(writer, cycle);

        Visit visit = (Visit) getVisit();
        Integer userPK = visit.getUserPK();

        VirtualLibraryEngine vengine = (VirtualLibraryEngine) getEngine();

        for (int i = 0; i < 2; i++)
        {
            try
            {
                IBookQuery query = getBorrowedQuery();
                int count = query.borrowerQuery(userPK);

                if (count != browser.getResultCount())
                    browser.initializeForResultCount(count);

                break;
            }
            catch (RemoteException ex)
            {
                vengine.rmiFailure("Remote exception finding borrowed books.", ex, i > 0);

                setBorrowedQuery(null);
            }
        }
    }

    public void setBorrowedQuery(IBookQuery value)
    {
        borrowedQuery = value;

        fireObservedChange("borrowedQuery", value);
    }

    public IBookQuery getBorrowedQuery()
    {
        if (borrowedQuery == null)
        {
            VirtualLibraryEngine vengine = (VirtualLibraryEngine) getEngine();
            setBorrowedQuery(vengine.createNewQuery());
        }

        return borrowedQuery;
    }

    /**
     *  Updates the currentBook dynamic page property.
     *
     **/

    public void setCurrentBook(Book value)
    {
        currentBook = value;
    }

    public Book getCurrentBook()
    {
        return currentBook;
    }

    public void setMessage(String value)
    {
        message = value;
    }

    public String getMessage()
    {
        return message;
    }

    /**
     *  Listener used to return a book.
     *
     **/

    public void returnBook(IRequestCycle cycle)
    {
        Object[] parameters = cycle.getServiceParameters();
        Integer bookPK = (Integer) parameters[0];

        VirtualLibraryEngine vengine = (VirtualLibraryEngine) getEngine();
        IOperations operations = vengine.getOperations();

        try
        {
            Book book = operations.returnBook(bookPK);

            setMessage("Returned book: " + book.getTitle());
        }
        catch (FinderException ex)
        {
            setError("Could not return book: " + ex.getMessage());
            return;
        }
        catch (RemoteException ex)
        {
            throw new ApplicationRuntimeException(ex);
        }
    }

}