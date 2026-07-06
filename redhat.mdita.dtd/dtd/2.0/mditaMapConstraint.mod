<?xml version="1.0" encoding="UTF-8"?>
<!-- ============================================================= -->
<!--  MODULE:    MDITA Map Content Constraint (DITA 2.0)           -->
<!--  PURPOSE:   Restrict DITA map content models to elements      -->
<!--             representable in MDITA extended profile maps       -->
<!-- ============================================================= -->

<!-- NOTE: This module is included AFTER shell DTD domain extensions
     but BEFORE map.mod. Entity references like %topicref; and %data;
     are defined in the shell. Bare element names (title, topicmeta,
     reltable) are used for entities not yet defined. -->

<!-- Override %map.content; — remove navref, keep title, topicmeta, reltable, topicref -->
<!ENTITY % map.content
              "(title?,
                topicmeta?,
                (reltable |
                 %topicref;)*)"
>

<!-- Override %topicref.content; — remove navref -->
<!ENTITY % topicref.content
              "(topicmeta?,
                (%data; |
                 %topicref;)*)"
>
