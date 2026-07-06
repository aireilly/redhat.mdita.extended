<?xml version="1.0" encoding="UTF-8"?>
<!-- ============================================================= -->
<!--  MODULE:    MDITA Map Content Constraint (DITA 1.3)           -->
<!--  PURPOSE:   Restrict DITA 1.3 map content models to elements  -->
<!--             representable in MDITA extended profile maps       -->
<!-- ============================================================= -->

<!ENTITY % map.content
              "(title?,
                topicmeta?,
                (reltable |
                 %topicref;)*)"
>

<!ENTITY % topicref.content
              "(topicmeta?,
                (%data; |
                 %topicref;)*)"
>
