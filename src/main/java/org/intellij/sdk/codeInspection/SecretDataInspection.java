// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.sdk.codeInspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiAssignmentExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class SecretDataInspection extends AbstractBaseJavaLocalInspectionTool {

  private final ReplaceWithStarsForAssignmentQuickFix myQuickFix = new ReplaceWithStarsForAssignmentQuickFix();
  private final ReplaceWithStarsForLiteralQuickFix myQuickFix2 = new ReplaceWithStarsForLiteralQuickFix();

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {

      @Override
      public void visitAssignmentExpression(@NotNull PsiAssignmentExpression expression) {
        PsiExpression lOperand = expression.getLExpression();
        PsiExpression rOperand = expression.getRExpression();
        if (rOperand == null || isNullLiteral(lOperand) || isNullLiteral(rOperand)) {
          return;
        }
        if ((isStringType(lOperand) || isStringType(rOperand)) && !Objects.equals(rOperand.getText(), "\"***\"")) {
          holder.registerProblem(expression,
                  InspectionBundle.message("inspection.comparing.string.references.problem.descriptor"),
                  myQuickFix);
        }
      }

      @Override
      public void visitLiteralExpression(PsiLiteralExpression node) {
        if (isNullLiteral(node)) {
          return;
        }

        if (node.getValue() instanceof String && PsiVariable.class.isAssignableFrom(node.getParent().getClass())
          && checkVarName(((PsiVariable) node.getParent()).getName()) && !Objects.equals(node.getValue(), "***")) {
          System.out.println(node.getValue());
          holder.registerProblem(node,
                  InspectionBundle.message("inspection.comparing.string.references.problem.descriptor"),
                  myQuickFix2);
        }
      }

      private boolean isStringType(PsiExpression operand) {
        PsiClass psiClass = PsiTypesUtil.getPsiClass(operand.getType());
        if (psiClass == null) {
          return false;
        }

        return "java.lang.String".equals(psiClass.getQualifiedName());
      }

      private static boolean isNullLiteral(PsiExpression expression) {
        return expression instanceof PsiLiteralExpression &&
            ((PsiLiteralExpression) expression).getValue() == null;
      }

      private static boolean checkVarName(String varName)  {
        return Objects.equals(varName, "password");
      }
    };
  }

  private static class ReplaceWithStarsForAssignmentQuickFix implements LocalQuickFix {

    @NotNull
    @Override
    public String getName() {
      return InspectionBundle.message("inspection.comparing.string.references.use.quickfix");
    }

    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiAssignmentExpressionImpl assignmentExpression = (PsiAssignmentExpressionImpl) descriptor.getPsiElement();
      PsiExpression lExpr = assignmentExpression.getLExpression();
      PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
      PsiAssignmentExpressionImpl assignCall =
              (PsiAssignmentExpressionImpl) factory.createExpressionFromText("a = \"***\"", null);
      PsiExpression qualifierExpression =
              assignCall.getLExpression();
      qualifierExpression.replace(lExpr);
      assignmentExpression.replace(assignCall);
    }

    @NotNull
    public String getFamilyName() {
      return getName();
    }
  }

  private static class ReplaceWithStarsForLiteralQuickFix implements LocalQuickFix {

    @NotNull
    @Override
    public String getName() {
      return InspectionBundle.message("inspection.comparing.string.references.use.quickfix");
    }

    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiLiteralExpressionImpl literalExpression = (PsiLiteralExpressionImpl) descriptor.getPsiElement();
      literalExpression.updateText("\"***\"");
    }

    @NotNull
    public String getFamilyName() {
      return getName();
    }
  }
}
