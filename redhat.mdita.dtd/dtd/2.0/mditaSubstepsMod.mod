<?xml version="1.0" encoding="UTF-8"?>
<!-- ============================================================= -->
<!--  MODULE:    MDITA Substeps Re-declaration (DITA 2.0)          -->
<!--  PURPOSE:   Re-declare substeps/substep elements removed in   -->
<!--             DITA 2.0 but still produced by redhat.mdita.extended -->
<!-- ============================================================= -->

<!-- NOTE: This module is included AFTER task.mod, so %cmd;, %info;,
     %note;, %conref-atts;, %select-atts;, %localization-atts; are
     all available. -->

<!ENTITY % substeps    "substeps">
<!ENTITY % substep     "substep">

<!ENTITY % substeps.content
              "((%substep;)+)"
>
<!ENTITY % substeps.attributes
              "id         ID                               #IMPLIED
               %conref-atts;
               %select-atts;
               %localization-atts;
               outputclass  CDATA                          #IMPLIED"
>
<!ELEMENT  substeps %substeps.content;>
<!ATTLIST  substeps %substeps.attributes;>
<!ATTLIST  substeps
              class  CDATA "- topic/ol task/substeps ">

<!ENTITY % substep.content
              "((%note;)*,
                (%cmd;),
                (%info;)*)"
>
<!ENTITY % substep.attributes
              "id         ID                               #IMPLIED
               %conref-atts;
               %select-atts;
               %localization-atts;
               outputclass  CDATA                          #IMPLIED"
>
<!ELEMENT  substep %substep.content;>
<!ATTLIST  substep %substep.attributes;>
<!ATTLIST  substep
              class  CDATA "- topic/li task/substep ">
