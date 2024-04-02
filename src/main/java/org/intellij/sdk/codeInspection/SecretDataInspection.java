// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.sdk.codeInspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiAssignmentExpressionImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class SecretDataInspection extends AbstractBaseJavaLocalInspectionTool {

  private final ReplaceWithEqualsQuickFix myQuickFix = new ReplaceWithEqualsQuickFix();

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {

      @Override
      public void visitBinaryExpression(@NotNull PsiBinaryExpression expression) {
        super.visitBinaryExpression(expression);
        IElementType opSign = expression.getOperationTokenType();
        if (opSign == JavaTokenType.LE) {
          System.out.println(opSign);
          PsiExpression lOperand = expression.getLOperand();
          PsiExpression rOperand = expression.getROperand();
          if (rOperand == null || isNullLiteral(lOperand) || isNullLiteral(rOperand)) {
            return;
          }
          if (isStringType(lOperand) && isStringType(rOperand) && !Objects.equals(rOperand.getText(), "***")) {
            holder.registerProblem(expression,
                InspectionBundle.message("inspection.comparing.string.references.problem.descriptor"),
                myQuickFix);
          }
        }
      }

      @Override
      public void visitAssignmentExpression(@NotNull PsiAssignmentExpression expression) {
        super.visitAssignmentExpression(expression);
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
    };
  }

  private static class ReplaceWithEqualsQuickFix implements LocalQuickFix {

    @NotNull
    @Override
    public String getName() {
      return InspectionBundle.message("inspection.comparing.string.references.use.quickfix");
    }

    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiAssignmentExpressionImpl binaryExpression = (PsiAssignmentExpressionImpl) descriptor.getPsiElement();
        PsiExpression lExpr = binaryExpression.getLExpression();
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
      PsiAssignmentExpressionImpl assignCall =
                (PsiAssignmentExpressionImpl) factory.createExpressionFromText("a = \"***\"", null);
        PsiExpression qualifierExpression =
                assignCall.getLExpression();
        qualifierExpression.replace(lExpr);
        binaryExpression.replace(assignCall);
    }

    @NotNull
    public String getFamilyName() {
      return getName();
    }
  }
}
