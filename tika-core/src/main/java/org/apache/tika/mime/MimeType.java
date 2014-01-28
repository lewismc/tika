/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.mime;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internet media type.
 */
public final class MimeType implements Comparable<MimeType>, Serializable {

    /**
     * Generated serial ID
     */
    private static final long serialVersionUID = 4357830439860729201L;
    
    private final static String MSG = "Cannot parse MIME type (expected type/subtype[;q=x.y] format): ";

    /**
     * Checks that the given string is a valid Internet media type name
     * based on rules from RFC 2054 section 5.3. For validation purposes the
     * rules can be simplified to the following:
     * <pre>
     * name := token "/" token
     * token := 1*&lt;any (US-ASCII) CHAR except SPACE, CTLs, or tspecials&gt;
     * tspecials :=  "(" / ")" / "&lt;" / "&gt;" / "@" / "," / ";" / ":" /
     *               "\" / <"> / "/" / "[" / "]" / "?" / "="
     * </pre>
     *
     * @param name name string
     * @return <code>true</code> if the string is a valid media type name,
     *         <code>false</code> otherwise
     */
    public static boolean isValid(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is missing");
        }

        boolean slash = false;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch <= ' ' || ch >= 127 || ch == '(' || ch == ')' ||
                    ch == '<' || ch == '>' || ch == '@' || ch == ',' ||
                    ch == ';' || ch == ':' || ch == '\\' || ch == '"' ||
                    ch == '[' || ch == ']' || ch == '?' || ch == '=') {
                return false;
            } else if (ch == '/') {
                if (slash || i == 0 || i + 1 == name.length()) {
                    return false;
                }
                slash = true;
            }
        }
        return slash;
    }

    /**
     * The normalized media type name.
     */
    private final MediaType type;

    /**
     * The MimeType acronym
     */
    private String acronym = "";

    /**
     * The http://en.wikipedia.org/wiki/Uniform_Type_Identifier
     */
    private String uti = "";
    
    /**
     * Documentation Links
     */
    private List<URI> links = Collections.emptyList();
    
    /**
     * Description of this media type.
     */
    private String description = "";

    /** The magics associated to this Mime-Type */
    private List<Magic> magics = null;

    /** The root-XML associated to this Mime-Type */
    private List<RootXML> rootXML = null;

    /** The minimum length of data to provides for magic analyzis */
    private int minLength = 0;

    /**
     * All known file extensions of this type, in order of preference
     * (best first).
     */
    private List<String> extensions = null;

    /**
     * The normalized media subtype name.
     */
    private String subtype  = null;
    
    /**
     * The quality value for the media type.
     */
    private double q = 0;

    /**
     * Creates a media type with the give name and containing media type
     * registry. The name is expected to be valid and normalized to lower
     * case. This constructor should only be called by
     * {@link MimeTypes#forName(String)} to keep the media type registry
     * up to date.
     *
     * @param type normalized media type name
     */
    MimeType(MediaType type) {
        if (type == null) {
            throw new IllegalArgumentException("Media type name is missing");
        }
        this.type = type;
    }
    
    /**
     * Creates a media type with the give name and containing media type
     * registry. The name is expected to be valid and normalized to lower
     * case. This constructor should only be called by
     * {@link MimeTypes#forName(String)} to keep the media type registry
     * up to date. In addition parameters for media subtype and quality
     * are provided.
     * An example of a mime type containg these parameters is: 
     * <code>application/rdf+xml;q=0.9</code> 
     * @param type normalized media type name
     * @param subtype normalized media subtype name
     * @param q quality value for the media type
     */
    MimeType(MediaType type, String subtype, double q) {
      if (type == null) {
        throw new IllegalArgumentException("Media type name is missing");
      }
      this.type = type;
      this.subtype = subtype;
      this.q = q;
    }
    
    /**
     * Parses the given MIME type string returning an instance of
     * {@link MIMEType}.
     * The expected format for <code>mimeType</code> is
     * <code>type/subtype[;q=x.y]</code> .
     * An example of valid mime type is: <code>application/rdf+xml;q=0.9</code> 
     *
     * @param mimeType
     * @return the mime type instance.
     * @throws IllegalArgumentException if the <code>mimeType</code> is not well formatted.
     */
    public static MimeType parse(String mimeType) {
        if (mimeType == null) return null;
        int i = mimeType.indexOf(';');
        double q = 1.0;
        if (i > -1) {
            String[] params = mimeType.substring(i + 1).split(";");
            for (String param : params) {
                int i2 = param.indexOf('=');
                if (i2 == -1) continue;
                if (!"q".equals(param.substring(0, i2).trim().toLowerCase())) continue;
                String value = param.substring(i2 + 1);
                try {
                    q = Double.parseDouble(value);
                } catch (NumberFormatException ex) {
                    continue;
                }
                if (q <= 0.0 || q >= 1.0) {
                    q = 1.0;
                }
            }
        } else {
            i = mimeType.length();
        }
        String type = mimeType.substring(0, i);
        int i2 = type.indexOf('/');
        if (i2 == -1) {
            throw new IllegalArgumentException(MSG + mimeType);
        }
        String p1 = type.substring(0, i2).trim().toLowerCase();
        String p2 = type.substring(i2 + 1).trim().toLowerCase();
        if ("*".equals(p1)) {
            if (!"*".equals(p2)) {
                throw new IllegalArgumentException(MSG + mimeType);
            }
            return new MimeType(null, null, q);
        }
        if ("*".equals(p2)) {
            return new MimeType(MediaType.parse(p1), null, q);
        }
        return new MimeType(MediaType.parse(p1), p2, q);
    }

    /**
     * Returns the normalized media type name.
     *
     * @return media type
     */
    public MediaType getType() {
        return type;
    }

    /**
     * Returns the name of this media type.
     *
     * @return media type name (lower case)
     */
    public String getName() {
        return type.toString();
    }

    /**
     * Returns the description of this media type.
     *
     * @return media type description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of this media type.
     *
     * @param description media type description
     */
    public void setDescription(String description) {
        if (description == null) {
            throw new IllegalArgumentException("Description is missing");
        }
        this.description = description;
    }
    

    /**
     * Returns an acronym for this mime type.
     *
     * @return mime type acronym
     */
    public String getAcronym() {
        return acronym;
    }

    /**
     * Set an acronym for the mime type
     *
     * @param acronym
     */
    void setAcronym(String v) {
        if (v == null) {
            throw new IllegalArgumentException("Acronym is missing");
        }
        acronym = v;
    }
    
    /**
     * Get the UTI for this mime type.
     * 
     * @see http://en.wikipedia.org/wiki/Uniform_Type_Identifier
     * 
     * @return The Uniform Type Identifier
     */
    public String getUniformTypeIdentifier() {
        return uti;
    }

    /**
     * Set The Uniform Type Identifier
     *
     * @param uti
     */
    void setUniformTypeIdentifier(String v) {
        if (v == null) {
            throw new IllegalArgumentException("Uniform Type Identifier is missing");
        }
        uti = v;
    }

    /**
     * Get a list of links to help document this mime type
     * 
     * @return an array of links (will never be null)
     */
    public List<URI> getLinks() {
      return links; // this is already unmodifiable
    }

    /**
     * Add a link to this mime type
     * @param link
     */
    void addLink(URI link) {
        if(link==null) {
            throw new IllegalArgumentException("Missing Link");
        }
        List<URI> copy = new ArrayList<URI>(links.size()+1);
        copy.addAll(links);
        copy.add(link);
        links = Collections.unmodifiableList(copy);
    }


    /**
     * Add some rootXML info to this mime-type
     *
     * @param namespaceURI
     * @param localName
     */
    void addRootXML(String namespaceURI, String localName) {
        if (rootXML == null) {
            rootXML = new ArrayList<RootXML>();
        }
        rootXML.add(new RootXML(this, namespaceURI, localName));
    }

    boolean matchesXML(String namespaceURI, String localName) {
        if (rootXML != null) {
            for (RootXML xml : rootXML) {
                if (xml.matches(namespaceURI, localName)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean hasRootXML() {
        return rootXML != null;
    }

    List<Magic> getMagics() {
        if (magics != null) {
            return magics;
        } else {
            return Collections.emptyList();
        }
    }

    void addMagic(Magic magic) {
        if (magic == null) {
            return;
        }
        if (magics == null) {
            magics = new ArrayList<Magic>();
        }
        magics.add(magic);
    }

    int getMinLength() {
        return minLength;
    }

    public boolean hasMagic() {
        return magics != null;
    }

    public boolean matchesMagic(byte[] data) {
        for (int i = 0; magics != null && i < magics.size(); i++) {
            Magic magic = magics.get(i);
            if (magic.eval(data)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(byte[] data) {
        return matchesMagic(data);
    }

    /**
     * Defines a RootXML description. RootXML is made of a localName and/or a
     * namespaceURI.
     */
    static class RootXML implements Serializable {

        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 5140496601491000730L;

        private MimeType type = null;

        private String namespaceURI = null;

        private String localName = null;

        RootXML(MimeType type, String namespaceURI, String localName) {
            if (isEmpty(namespaceURI) && isEmpty(localName)) {
                throw new IllegalArgumentException(
                        "Both namespaceURI and localName cannot be empty");
            }
            this.type = type;
            this.namespaceURI = namespaceURI;
            this.localName = localName;
        }

        boolean matches(String namespaceURI, String localName) {
            //Compare namespaces
            if (!isEmpty(this.namespaceURI)) {
                if (!this.namespaceURI.equals(namespaceURI)) {
                    return false;
                }
            }
            else{
                // else if it was empty then check to see if the provided namespaceURI
                // is empty. If it is not, then these two aren't equal and return false
                if(!isEmpty(namespaceURI)){
                    return false;
                }
            }

            //Compare root element's local name
            if (!isEmpty(this.localName)) {
                if (!this.localName.equals(localName)) {
                    return false;
                }
            }
            else{
                // else if it was empty then check to see if the provided localName
                // is empty. If it is not, then these two aren't equal and return false 
                if(!isEmpty(localName)){
                    return false;
                }
            }
            return true;
        }

        /**
         * Checks if a string is null or empty.
         */
        private boolean isEmpty(String str) {
            return (str == null) || (str.equals(""));
        }

        MimeType getType() {
            return type;
        }

        String getNameSpaceURI() {
            return namespaceURI;
        }

        String getLocalName() {
            return localName;
        }

        public String toString() {
            return type + ", " + namespaceURI + ", " + localName;
        }
    }

    //----------------------------------------------------------< Comparable >

    public int compareTo(MimeType mime) {
        return type.compareTo(mime.type);
    }

    //--------------------------------------------------------------< Object >

    public boolean equals(Object o) {
        if (o instanceof MimeType) {
            MimeType that = (MimeType) o;
            return this.type.equals(that.type);
        }

        return false;
    }

    public int hashCode() {
        return type.hashCode();
    }

    /**
     * Returns the name of this media type.
     *
     * @return media type name
     */
    public String toString() {
        return type.toString();
    }

    /**
     * Returns the preferred file extension of this type, or an empty string
     * if no extensions are known. Use the {@link #getExtensions()} method to
     * get the full list of known extensions of this type.
     *
     * @since Apache Tika 0.9
     * @return preferred file extension or empty string
     */
    public String getExtension() {
        if (extensions == null) {
            return "";
        } else {
            return extensions.get(0);
        }
    }

    /**
     * Returns the list of all known file extensions of this media type.
     *
     * @since Apache Tika 0.10
     * @return known extensions in order of preference (best first)
     */
    public List<String> getExtensions() {
        if (extensions != null) {
            return Collections.unmodifiableList(extensions);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Adds a known file extension to this type.
     *
     * @param extension file extension
     */
    void addExtension(String extension) {
        if (extensions == null) {
            extensions = Collections.singletonList(extension);
        } else if (extensions.size() == 1) {
            extensions = new ArrayList<String>(extensions);
        }
        if (!extensions.contains(extension)) {
            extensions.add(extension);
        }
    }

    public String getMajorType() {
      return (type.toString() == null ? "*" : type.toString());
    }
    
    public String getSubtype() {
      return (subtype == null ? "*" : subtype);
  }

    public String getFullType() {
      return getMajorType() + "/" + getSubtype();
    }

    public double getQuality() {
      return q;
    }

    public boolean isAnyMajorType() {
      return type == null;
    }

    public boolean isAnySubtype() {
      return subtype == null;
    }

}
