<?xml version="1.0" encoding="UTF-8"?>
<!-- ============================================================= -->
<!--  MODULE:    MDITA Topic Content Constraint (DITA 1.3)         -->
<!--  PURPOSE:   Restrict DITA 1.3 topic content models to elements -->
<!--             representable in the MDITA extended profile        -->
<!-- ============================================================= -->

<!-- ============================================================= -->
<!--                    INLINE CONTENT OVERRIDES                   -->
<!-- ============================================================= -->

<!ENTITY % basic.ph
              "cite |
               %keyword; |
               %ph; |
               xref"
>

<!ENTITY % basic.ph.noxref.nocite
              "%keyword; |
               %ph;"
>

<!ENTITY % basic.ph.noxref
              "cite |
               %keyword; |
               %ph;"
>

<!ENTITY % basic.ph.notm
              "cite |
               %keyword; |
               %ph; |
               xref"
>

<!-- ============================================================= -->
<!--                    BLOCK CONTENT OVERRIDES                    -->
<!-- ============================================================= -->

<!ENTITY % basic.block
              "%dl; |
               %fig; |
               image |
               lq |
               %note; |
               ol |
               p |
               %pre; |
               simpletable |
               ul"
>

<!ENTITY % basic.block.noexample
              "%dl; |
               %fig; |
               image |
               lq |
               %note; |
               ol |
               p |
               %pre; |
               simpletable |
               ul"
>

<!ENTITY % basic.block.nopara
              "%dl; |
               %fig; |
               image |
               lq |
               %note; |
               ol |
               %pre; |
               simpletable |
               ul"
>

<!ENTITY % basic.block.nonote
              "%dl; |
               %fig; |
               image |
               lq |
               ol |
               p |
               %pre; |
               simpletable |
               ul"
>

<!ENTITY % basic.block.nolq
              "%dl; |
               %fig; |
               image |
               %note; |
               ol |
               p |
               %pre; |
               simpletable |
               ul"
>

<!ENTITY % basic.block.notbl
              "%dl; |
               %fig; |
               image |
               lq |
               %note; |
               ol |
               p |
               %pre; |
               ul"
>

<!ENTITY % basic.block.notbnofg
              "%dl; |
               image |
               lq |
               %note; |
               ol |
               p |
               %pre; |
               ul"
>

<!ENTITY % basic.block.notbfgobj
              "%dl; |
               image |
               lq |
               %note; |
               ol |
               p |
               %pre; |
               ul"
>

<!-- ============================================================= -->
<!--                    INCLUSION / MISC OVERRIDES                 -->
<!-- ============================================================= -->

<!ENTITY % txt.incl
              "required-cleanup"
>

<!ENTITY % foreign.unknown.incl
              "%foreign;"
>

<!ENTITY % data.elements.incl
              "%data;"
>

<!-- ============================================================= -->
<!--                    COMPOUND CONTENT MODEL OVERRIDES            -->
<!-- ============================================================= -->

<!ENTITY % body.cnt
              "%dl; |
               %fig; |
               image |
               lq |
               %note; |
               ol |
               p |
               %pre; |
               simpletable |
               ul |
               %data; |
               required-cleanup"
>

<!ENTITY % section.cnt
              "#PCDATA |
               %dl; |
               %fig; |
               image |
               lq |
               %note; |
               ol |
               p |
               %pre; |
               simpletable |
               ul |
               cite |
               %keyword; |
               %ph; |
               xref |
               %data; |
               title |
               required-cleanup"
>

<!ENTITY % section.notitle.cnt
              "#PCDATA |
               %dl; |
               %fig; |
               image |
               lq |
               %note; |
               ol |
               p |
               %pre; |
               simpletable |
               ul |
               cite |
               %keyword; |
               %ph; |
               xref |
               %data; |
               required-cleanup"
>

<!ENTITY % ph.cnt
              "#PCDATA |
               cite |
               %keyword; |
               %ph; |
               xref |
               %data; |
               image |
               required-cleanup"
>

<!-- ============================================================= -->
<!--                    TOPIC STRUCTURE OVERRIDES                  -->
<!-- ============================================================= -->

<!ENTITY % topic.content
              "(title,
                shortdesc?,
                prolog?,
                body?,
                (%topic-info-types;)*)"
>

<!ENTITY % body.content
              "(%body.cnt; |
                section)*"
>
