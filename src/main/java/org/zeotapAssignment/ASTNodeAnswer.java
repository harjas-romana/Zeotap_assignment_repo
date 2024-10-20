package org.zeotapAssignment;
import java.util.*;
import org.json.JSONObject;

class ASTNode {
    private String type;
    private ASTNode left;
    private ASTNode right;
    private Object value;

    // Parameterized constructor
    public ASTNode(String type, ASTNode left, ASTNode right, Object value) {
        this.type = type;
        this.left = left;
        this.right = right;
        this.value = value;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ASTNode getLeft() {
        return left;
    }

    public void setLeft(ASTNode left) {
        this.left = left;
    }

    public ASTNode getRight() {
        return right;
    }

    public void setRight(ASTNode right) {
        this.right = right;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    // Methods
    public ASTNode createRule(String ruleString) {
        Stack<ASTNode> stack = new Stack<>();
        StringBuilder token = new StringBuilder();
        int nestedParens = 0;

        for (char ch : ruleString.toCharArray()) {
            if (ch == '(') {
                if (nestedParens > 0) {
                    token.append(ch);
                }
                nestedParens++;
            } else if (ch == ')') {
                nestedParens--;
                if (nestedParens > 0) {
                    token.append(ch);
                } else if (nestedParens == 0 && token.length() > 0) {
                    // Create a subexpression node
                    ASTNode subExpressionNode = createRule(token.toString().trim());
                    stack.push(subExpressionNode);
                    token.setLength(0);
                }
            } else if (nestedParens > 0 || (!Character.isWhitespace(ch) && ch != '(' && ch != ')')) {
                token.append(ch);
            } else if (Character.isWhitespace(ch) && token.length() > 0 && nestedParens == 0) {
                // Process token outside of nested parentheses
                String currentToken = token.toString().trim();
                if (currentToken.equals("AND") || currentToken.equals("OR")) {
                    ASTNode operatorNode = new ASTNode("operator", null, null, currentToken);
                    stack.push(operatorNode);
                } else {
                    // Check if the token is a condition or a single attribute
                    if (currentToken.contains(">") || currentToken.contains("<") || currentToken.contains("=")) {
                        // Split the token into attribute, operator, and value
                        String[] parts = currentToken.split(" ");
                        if (parts.length == 3) {
                            String attribute = parts[0];
                            String operator = parts[1];
                            String value = parts[2];
                            ASTNode operandNode = new ASTNode("operand", null, null, attribute + " " + operator + " " + value);
                            stack.push(operandNode);
                        } else if (parts.length == 1) {
                            // Handle single operator
                            ASTNode operatorNode = new ASTNode("operator", null, null, currentToken);
                            stack.push(operatorNode);
                        } else {
                            // Handle invalid condition format
                            throw new IllegalArgumentException("Invalid condition format: " + currentToken);
                        }
                    } else {
                        // Handle single attribute
                        ASTNode operandNode = new ASTNode("operand", null, null, currentToken);
                        stack.push(operandNode);
                    }
                }
                token.setLength(0);
            }
        }
        if (token.length() > 0) {
            String finalToken = token.toString().trim();
            // Check if the token is a condition or a single attribute
            if (finalToken.contains(">") || finalToken.contains("<") || finalToken.contains("=")) {
                // Split the token into attribute, operator, and value
                String[] parts = finalToken.split(" ");
                if (parts.length == 3) {
                    String attribute = parts[0];
                    String operator = parts[1];
                    String value = parts[2];
                    ASTNode operandNode = new ASTNode("operand", null, null, attribute + " " + operator + " " + value);
                    stack.push(operandNode);
                } else if (parts.length == 1) {
                    // Handle single operator
                    ASTNode operatorNode = new ASTNode("operator", null, null, finalToken);
                    stack.push(operatorNode);
                } else {
                    // Handle invalid condition format
                    throw new IllegalArgumentException("Invalid condition format: " + finalToken);
                }
            } else {
                // Handle single attribute
                ASTNode operandNode = new ASTNode("operand", null, null, finalToken);
                stack.push(operandNode);
            }
        }

        ASTNode root = null;
        while (!stack.isEmpty()) {
            ASTNode node = stack.pop();
            if (root == null) {
                root = node;
            } else {
                ASTNode operatorNode = stack.pop();
                operatorNode.setRight(root);
                operatorNode.setLeft(node);
                root = operatorNode;
            }
        }

        return root;
    }

    public ASTNode combineRule(List<ASTNode> rules) {
        ASTNode combinedRoot = null;
        for (ASTNode rule : rules) {
            if (combinedRoot == null) {
                combinedRoot = rule;
            } else {
                ASTNode andNode = new ASTNode("operator", combinedRoot, rule, "AND");
                combinedRoot = andNode;
            }
        }
        return combinedRoot;
    }

    public boolean evaluateRule(ASTNode ast, Map<String, Object> data) {
        if (ast.getType().equals("operand")) {
            // Evaluate operand condition
            return evaluateCondition(ast.getValue(), data);
        } else if (ast.getType().equals("operator")) {
            // Evaluate operator
            boolean leftResult = evaluateRule(ast.getLeft(), data);
            boolean rightResult = evaluateRule(ast.getRight(), data);
            return applyOperator(ast.getValue(), leftResult, rightResult);
        }
        return false;
    }

    private boolean evaluateCondition(Object value, Map<String, Object> data) {
        // Split the condition into attribute, operator, and value
        String[] conditionParts = value.toString().split(" ");

        // Check if the condition is a single attribute
        if (conditionParts.length == 1) {
            String attribute = conditionParts[0];
            Object attributeValue = data.get(attribute);
            return attributeValue != null;
        }

        // Ensure that the condition is correctly parsed
        if (conditionParts.length < 3) {
            throw new IllegalArgumentException("Invalid condition format: " + value);
        }

        String attribute = conditionParts[0];
        String operator = conditionParts[1];
        String conditionValue = conditionParts[2];

        // Get the attribute value from the data
        Object attributeValue = data.get(attribute);

        if (attributeValue == null) {
            throw new IllegalArgumentException("Attribute " + attribute + " not found in data.");
        }

        // Evaluate the condition
        if (operator.equals(">")) {
            return ((Number) attributeValue).doubleValue() > Double.parseDouble(conditionValue);
        } else if (operator.equals("<")) {
            return ((Number) attributeValue).doubleValue() < Double.parseDouble(conditionValue);
        } else if (operator.equals("=")) {
            return attributeValue.toString().equals(conditionValue);
        }
        return false;
    }

    private boolean applyOperator(Object operator, boolean leftResult, boolean rightResult) {
        if (operator.equals("AND")) {
            return leftResult && rightResult;
        } else if (operator.equals("OR")) {
            return leftResult || rightResult;
        }
        return false;
    }
}

public class ASTNodeAnswer {
    public static void main(String[] args) {
        ASTNode astNode = new ASTNode("", null, null, null);

        // Create individual rules with balanced parentheses
        String rule1 = "((age > 30 AND department = 'Sales') OR (age < 25 AND department = 'Marketing')) AND (salary > 50000 OR experience > 5)";
        String rule2 = "(age > 30 AND department = 'Marketing') AND (salary > 20000 OR experience > 5)";

        // Create AST nodes for each rule
        ASTNode rule1Node = astNode.createRule(rule1);
        ASTNode rule2Node = astNode.createRule(rule2);
        List<ASTNode> rules = Arrays.asList(rule1Node, rule2Node);
        ASTNode combinedRoot = astNode.combineRule(rules);
        Map<String, Object> data = new HashMap<>();
        data.put("age", 35);
        data.put("department", "Sales");
        data.put("salary", 60000);
        data.put("experience", 3);
        boolean result = astNode.evaluateRule(combinedRoot, data);
        System.out.println("Result: " + result);

        // Convert data to JSON
        JSONObject jsonData = new JSONObject(data);
        System.out.println("JSON Data: " + jsonData.toString());
    }
}
