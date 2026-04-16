package com.elovirta.dita.markdown;

import static com.elovirta.dita.markdown.renderer.TopicRenderer.TIGHT_LIST_P;
import static javax.xml.XMLConstants.NULL_NS_URI;
import static org.dita.dost.util.Constants.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dita.dost.util.Constants;
import org.dita.dost.util.DitaClass;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

public class SpecializeFilter extends XMLFilterImpl {

  private static final int DEPTH_IN_BODY = 3;

  public enum Type {
    TOPIC,
    CONCEPT,
    TASK,
    REFERENCE,
  }

  private enum ReferenceState {
    BODY,
    SECTION,
  }

  private enum TaskState {
    BODY,
    CONTEXT,
    STEPS,
    STEP,
    INFO,
    SUBSTEPS,
    SUBSTEP,
    SUBINFO,
    CHOICES,
    CHOICETABLE,
    RESULT,
    POST_STEPS,
  }

  private final Type forceType;

  /**
   * Topic type stack. Default to topic in case of compound type
   */
  private final Deque<Type> typeStack = new ArrayDeque<>(List.of(Type.TOPIC));
  private int paragraphCountInStep = 0;
  private int paragraphCountInSubstep = 0;
  private int depth = 0;
  private TaskState taskState = null;
  private ReferenceState referenceState = null;
  private boolean stepsCompleted = false;
  private int choicetableColumn = 0;
  private boolean inChoicetableHead = false;

  private static final Map<String, DitaClass> TASK_SECTIONS = Map.of(
    TASK_PREREQ.localName, TASK_PREREQ,
    TASK_CONTEXT.localName, TASK_CONTEXT,
    TASK_RESULT.localName, TASK_RESULT,
    TASK_POSTREQ.localName, TASK_POSTREQ,
    TASK_TASKTROUBLESHOOTING.localName, TASK_TASKTROUBLESHOOTING
  );

  private final Deque<String> elementStack = new ArrayDeque<>();

  public SpecializeFilter() {
    this(null);
  }

  public SpecializeFilter(Type forceType) {
    super();
    this.forceType = forceType;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    depth++;

    if (localName.equals(TOPIC_TOPIC.localName)) {
      depth = 1;
      if (forceType != null) {
        typeStack.push(forceType);
      } else {
        final Collection<String> outputclasses = getOutputclass(atts);
        if (outputclasses.contains(CONCEPT_CONCEPT.localName)) {
          typeStack.push(Type.CONCEPT);
        } else if (outputclasses.contains(TASK_TASK.localName)) {
          typeStack.push(Type.TASK);
        } else if (outputclasses.contains(REFERENCE_REFERENCE.localName)) {
          typeStack.push(Type.REFERENCE);
        } else {
          typeStack.push(typeStack.peek());
        }
      }
    }

    switch (typeStack.peek()) {
      case CONCEPT:
        startElementConcept(uri, localName, qName, atts);
        break;
      case TASK:
        startElementTask(uri, localName, qName, atts);
        break;
      case REFERENCE:
        startElementReference(uri, localName, qName, atts);
        break;
      default:
        doStartElement(uri, localName, qName, atts);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    switch (typeStack.peek()) {
      case TASK:
        endElementTask(uri, localName, qName);
        break;
      case REFERENCE:
        endElementReference(uri, localName, qName);
        break;
      case CONCEPT:
        endElementConcept(uri, localName, qName);
        break;
      default:
        doEndElement(uri, localName, qName);
    }

    if (localName.equals(TOPIC_TOPIC.localName)) {
      typeStack.pop();
    }

    depth--;
  }

  private void startElementConcept(String uri, String localName, String qName, Attributes atts) throws SAXException {
    switch (localName) {
      case "topic":
        renameStartElement(Constants.CONCEPT_CONCEPT, atts);
        break;
      case "body":
        renameStartElement(Constants.CONCEPT_CONBODY, atts);
        break;
      default:
        doStartElement(uri, localName, qName, atts);
    }
  }

  private void endElementConcept(String uri, String localName, String qName) throws SAXException {
    doEndElement(uri, localName, qName);
  }

  private void startElementTask(String uri, String localName, String qName, Attributes atts) throws SAXException {
    switch (localName) {
      case "topic":
        renameStartElement(TASK_TASK, atts);
        taskState = null;
        stepsCompleted = false;
        break;
      case "body":
        taskState = TaskState.BODY;
        stepsCompleted = false;
        paragraphCountInSubstep = 0;
        choicetableColumn = 0;
        inChoicetableHead = false;
        renameStartElement(TASK_TASKBODY, atts);
        break;
      case "section":
        if (depth == DEPTH_IN_BODY) {
          closeImplicitSection(uri);
          final String outputclass = atts.getValue(ATTRIBUTE_NAME_OUTPUTCLASS);
          if (outputclass != null) {
            final DitaClass sectionClass = TASK_SECTIONS.get(outputclass.trim());
            if (sectionClass != null) {
              renameStartElement(sectionClass, atts);
              break;
            }
          }
          openImplicitSection(uri);
          doStartElement(uri, localName, qName, atts);
        } else {
          doStartElement(uri, localName, qName, atts);
        }
        break;
      case "ol":
        if (depth == DEPTH_IN_BODY) {
          closeImplicitSection(uri);
          taskState = TaskState.STEPS;
          renameStartElement(Constants.TASK_STEPS, atts);
        } else if (depth == 5 && (taskState == TaskState.STEP || taskState == TaskState.INFO)) {
          if (taskState == TaskState.INFO) {
            doEndElement(NULL_NS_URI, TASK_INFO.localName, TASK_INFO.localName);
          }
          taskState = TaskState.SUBSTEPS;
          renameStartElement(TASK_SUBSTEPS, atts);
        } else {
          doStartElement(uri, localName, qName, atts);
        }
        break;
      case "ul":
        if (depth == DEPTH_IN_BODY) {
          closeImplicitSection(uri);
          taskState = TaskState.STEPS;
          renameStartElement(TASK_STEPS_UNORDERED, atts);
        } else if (depth == 5 && (taskState == TaskState.STEP || taskState == TaskState.INFO)) {
          if (taskState == TaskState.INFO) {
            doEndElement(NULL_NS_URI, TASK_INFO.localName, TASK_INFO.localName);
          }
          taskState = TaskState.CHOICES;
          renameStartElement(TASK_CHOICES, atts);
        } else {
          doStartElement(uri, localName, qName, atts);
        }
        break;
      case "li":
        if (taskState == TaskState.STEPS && depth == 4) {
          renameStartElement(TASK_STEP, atts);
          taskState = TaskState.STEP;
        } else if (taskState == TaskState.SUBSTEPS && depth == 6) {
          renameStartElement(TASK_SUBSTEP, atts);
          taskState = TaskState.SUBSTEP;
        } else if (taskState == TaskState.CHOICES && depth == 6) {
          renameStartElement(TASK_CHOICE, atts);
        } else {
          doStartElement(uri, localName, qName, atts);
        }
        break;
      case "simpletable":
        if (depth == 5 && (taskState == TaskState.STEP || taskState == TaskState.INFO)) {
          if (taskState == TaskState.INFO) {
            doEndElement(NULL_NS_URI, TASK_INFO.localName, TASK_INFO.localName);
          }
          taskState = TaskState.CHOICETABLE;
          choicetableColumn = 0;
          inChoicetableHead = false;
          renameStartElement(TASK_CHOICETABLE, atts);
        } else {
          doStartElement(uri, localName, qName, atts);
        }
        break;
      case "sthead":
        if (taskState == TaskState.CHOICETABLE) {
          inChoicetableHead = true;
          choicetableColumn = 0;
          renameStartElement(TASK_CHHEAD, atts);
        } else {
          doStartElement(uri, localName, qName, atts);
        }
        break;
      case "strow":
        if (taskState == TaskState.CHOICETABLE) {
          inChoicetableHead = false;
          choicetableColumn = 0;
          renameStartElement(TASK_CHROW, atts);
        } else {
          doStartElement(uri, localName, qName, atts);
        }
        break;
      case "stentry":
        if (taskState == TaskState.CHOICETABLE) {
          choicetableColumn++;
          DitaClass entryClass;
          if (inChoicetableHead) {
            entryClass = choicetableColumn == 1 ? TASK_CHOPTIONHD : TASK_CHDESCHD;
          } else {
            entryClass = choicetableColumn == 1 ? TASK_CHOPTION : TASK_CHDESC;
          }
          renameStartElement(entryClass, atts);
        } else {
          doStartElement(uri, localName, qName, atts);
        }
        break;
      default:
        if (depth == DEPTH_IN_BODY) {
          openImplicitSection(uri);
          doStartElement(uri, localName, qName, atts);
        } else if ((taskState == TaskState.STEP || taskState == TaskState.INFO) && depth == 5) {
          switch (localName) {
            case "p":
            case TIGHT_LIST_P:
              paragraphCountInStep++;
              if (paragraphCountInStep == 1) {
                renameStartElement(TASK_CMD, atts);
              } else if (paragraphCountInStep == 2 && taskState != TaskState.INFO) {
                AttributesImpl res = new AttributesImpl(atts);
                res.addAttribute(
                  NULL_NS_URI,
                  ATTRIBUTE_NAME_CLASS,
                  ATTRIBUTE_NAME_CLASS,
                  "CDATA",
                  TASK_INFO.toString()
                );
                doStartElement(NULL_NS_URI, TASK_INFO.localName, TASK_INFO.localName, res);
                taskState = TaskState.INFO;
                doStartElement(uri, localName, qName, atts);
              } else {
                doStartElement(uri, localName, qName, atts);
              }
              break;
            default:
              if (taskState != TaskState.INFO) {
                AttributesImpl res = new AttributesImpl(atts);
                res.addAttribute(
                  NULL_NS_URI,
                  ATTRIBUTE_NAME_CLASS,
                  ATTRIBUTE_NAME_CLASS,
                  "CDATA",
                  TASK_INFO.toString()
                );
                doStartElement(NULL_NS_URI, TASK_INFO.localName, TASK_INFO.localName, res);
                taskState = TaskState.INFO;
              }
              doStartElement(uri, localName, qName, atts);
              break;
          }
        } else if ((taskState == TaskState.SUBSTEP || taskState == TaskState.SUBINFO) && depth == 7) {
          switch (localName) {
            case "p":
            case TIGHT_LIST_P:
              paragraphCountInSubstep++;
              if (paragraphCountInSubstep == 1) {
                renameStartElement(TASK_CMD, atts);
              } else if (paragraphCountInSubstep == 2 && taskState != TaskState.SUBINFO) {
                AttributesImpl res = new AttributesImpl(atts);
                res.addAttribute(
                  NULL_NS_URI,
                  ATTRIBUTE_NAME_CLASS,
                  ATTRIBUTE_NAME_CLASS,
                  "CDATA",
                  TASK_INFO.toString()
                );
                doStartElement(NULL_NS_URI, TASK_INFO.localName, TASK_INFO.localName, res);
                taskState = TaskState.SUBINFO;
                doStartElement(uri, localName, qName, atts);
              } else {
                doStartElement(uri, localName, qName, atts);
              }
              break;
            default:
              if (taskState != TaskState.SUBINFO) {
                AttributesImpl res = new AttributesImpl(atts);
                res.addAttribute(
                  NULL_NS_URI,
                  ATTRIBUTE_NAME_CLASS,
                  ATTRIBUTE_NAME_CLASS,
                  "CDATA",
                  TASK_INFO.toString()
                );
                doStartElement(NULL_NS_URI, TASK_INFO.localName, TASK_INFO.localName, res);
                taskState = TaskState.SUBINFO;
              }
              doStartElement(uri, localName, qName, atts);
              break;
          }
        } else {
          doStartElement(uri, localName, qName, atts);
        }
    }
  }

  private void closeImplicitSection(String uri) throws SAXException {
    if (taskState == TaskState.CONTEXT) {
      doEndElement(uri, TASK_CONTEXT.localName, TASK_CONTEXT.localName);
      taskState = stepsCompleted ? TaskState.POST_STEPS : TaskState.BODY;
    } else if (taskState == TaskState.RESULT) {
      doEndElement(uri, TASK_RESULT.localName, TASK_RESULT.localName);
      taskState = TaskState.POST_STEPS;
    }
  }

  private void openImplicitSection(String uri) throws SAXException {
    if (!stepsCompleted && taskState == TaskState.BODY) {
      AttributesImpl sectionAtts = new AttributesImpl();
      sectionAtts.addAttribute(
        NULL_NS_URI,
        ATTRIBUTE_NAME_CLASS,
        ATTRIBUTE_NAME_CLASS,
        "CDATA",
        TASK_CONTEXT.toString()
      );
      doStartElement(uri, TASK_CONTEXT.localName, TASK_CONTEXT.localName, sectionAtts);
      taskState = TaskState.CONTEXT;
    } else if (stepsCompleted && taskState == TaskState.POST_STEPS) {
      AttributesImpl sectionAtts = new AttributesImpl();
      sectionAtts.addAttribute(
        NULL_NS_URI,
        ATTRIBUTE_NAME_CLASS,
        ATTRIBUTE_NAME_CLASS,
        "CDATA",
        TASK_RESULT.toString()
      );
      doStartElement(uri, TASK_RESULT.localName, TASK_RESULT.localName, sectionAtts);
      taskState = TaskState.RESULT;
    }
  }

  private void endElementTask(String uri, String localName, String qName) throws SAXException {
    switch (localName) {
      case "body":
        if (taskState == TaskState.CONTEXT) {
          taskState = null;
          doEndElement(uri, TASK_CONTEXT.localName, TASK_CONTEXT.localName);
        } else if (taskState == TaskState.RESULT) {
          taskState = null;
          doEndElement(uri, TASK_RESULT.localName, TASK_RESULT.localName);
        }
        doEndElement(uri, localName, qName);
        break;
      case "ol":
        if (depth == DEPTH_IN_BODY) {
          stepsCompleted = true;
          taskState = TaskState.POST_STEPS;
        } else if (depth == 5 && taskState == TaskState.SUBSTEPS) {
          taskState = TaskState.STEP;
        }
        doEndElement(uri, localName, qName);
        break;
      case "ul":
        if (depth == DEPTH_IN_BODY) {
          stepsCompleted = true;
          taskState = TaskState.POST_STEPS;
        } else if (depth == 5 && taskState == TaskState.CHOICES) {
          taskState = TaskState.STEP;
        }
        doEndElement(uri, localName, qName);
        break;
      case "li":
        if (taskState == TaskState.SUBINFO && depth == 6) {
          doEndElement(NULL_NS_URI, TASK_INFO.localName, TASK_INFO.localName);
          taskState = TaskState.SUBSTEP;
        }
        if (taskState == TaskState.SUBSTEP && depth == 6) {
          paragraphCountInSubstep = 0;
          taskState = TaskState.SUBSTEPS;
        }
        if (taskState == TaskState.INFO && depth == 4) {
          doEndElement(NULL_NS_URI, TASK_INFO.localName, TASK_INFO.localName);
          taskState = TaskState.STEP;
        }
        if (taskState == TaskState.STEP && depth == 4) {
          paragraphCountInStep = 0;
          taskState = TaskState.STEPS;
        }
        doEndElement(uri, localName, qName);
        break;
      case "simpletable":
        if (depth == 5 && taskState == TaskState.CHOICETABLE) {
          taskState = TaskState.STEP;
          inChoicetableHead = false;
          choicetableColumn = 0;
        }
        doEndElement(uri, localName, qName);
        break;
      case "sthead":
        if (taskState == TaskState.CHOICETABLE) {
          inChoicetableHead = false;
        }
        doEndElement(uri, localName, qName);
        break;
      default:
        doEndElement(uri, localName, qName);
    }
  }

  private void startElementReference(String uri, String localName, String qName, Attributes atts) throws SAXException {
    switch (localName) {
      case "topic":
        referenceState = null;
        renameStartElement(REFERENCE_REFERENCE, atts);
        break;
      case "body":
        renameStartElement(REFERENCE_REFBODY, atts);
        referenceState = ReferenceState.BODY;
        break;
      default:
        if (depth == DEPTH_IN_BODY) {
          switch (localName) {
            case "table":
            case "section":
              if (referenceState == ReferenceState.SECTION) {
                referenceState = ReferenceState.BODY;
                doEndElement(uri, "section", "section");
              }
              break;
            default:
              if (referenceState == ReferenceState.BODY) {
                AttributesImpl sectionAtts = new AttributesImpl();
                sectionAtts.addAttribute(
                  NULL_NS_URI,
                  ATTRIBUTE_NAME_CLASS,
                  ATTRIBUTE_NAME_CLASS,
                  "CDATA",
                  "- topic/section "
                );
                doStartElement(uri, TOPIC_SECTION.localName, TOPIC_SECTION.localName, sectionAtts);
                referenceState = ReferenceState.SECTION;
              }
              break;
          }
          doStartElement(uri, localName, qName, atts);
        } else {
          doStartElement(uri, localName, qName, atts);
        }
    }
  }

  private void endElementReference(String uri, String localName, String qName) throws SAXException {
    switch (localName) {
      case "body":
        if (referenceState == ReferenceState.SECTION) {
          referenceState = null;
          doEndElement(uri, TOPIC_SECTION.localName, TOPIC_SECTION.localName);
        }
        doEndElement(uri, localName, qName);
        break;
      default:
        doEndElement(uri, localName, qName);
    }
  }

  public void doStartElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    //        System.out.printf("<%s>%n", localName);
    super.startElement(uri, localName, qName, atts);
    elementStack.push(localName);
  }

  public void doEndElement(String uri, String localName, String qName) throws SAXException {
    final String l = elementStack.pop();
    //        System.out.printf("</%s = %s>%n", l, localName);
    super.endElement(uri, l, l);
  }

  void renameStartElement(DitaClass cls, Attributes atts) throws SAXException {
    AttributesImpl res = new AttributesImpl(atts);
    final int classIndex = res.getIndex(ATTRIBUTE_NAME_CLASS);
    if (classIndex != -1) {
      res.setValue(classIndex, cls.toString());
    } else {
      res.addAttribute(NULL_NS_URI, ATTRIBUTE_NAME_CLASS, ATTRIBUTE_NAME_CLASS, "CDATA", cls.toString());
    }
    final int outputClassIndex = res.getIndex(NULL_NS_URI, ATTRIBUTE_NAME_OUTPUTCLASS);
    if (outputClassIndex != -1) {
      final String outputClassValue = res.getValue(outputClassIndex).trim();
      if (outputClassValue.isEmpty()) {
        res.removeAttribute(outputClassIndex);
      } else {
        final List<String> outputClass = Stream
          .of(outputClassValue.split("\\s+"))
          .filter(token -> !token.equals(cls.localName))
          .collect(Collectors.toList());
        if (outputClass.isEmpty()) {
          res.removeAttribute(outputClassIndex);
        } else {
          res.setValue(outputClassIndex, String.join(" ", outputClass));
        }
      }
    }
    doStartElement(NULL_NS_URI, cls.localName, cls.localName, res);
  }

  private Collection<String> getOutputclass(Attributes atts) {
    final String outputclass = atts.getValue(ATTRIBUTE_NAME_OUTPUTCLASS);
    if (outputclass == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(outputclass.trim().split("\\s+"));
  }
}
