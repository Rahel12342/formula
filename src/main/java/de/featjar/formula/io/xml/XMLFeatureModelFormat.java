/*
 * Copyright (C) 2022 Sebastian Krieter, Elias Kuiter
 *
 * This file is part of formula.
 *
 * formula is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * formula is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with formula. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula> for further information.
 */
package de.featjar.formula.io.xml;

import de.featjar.formula.structure.Expression;
import de.featjar.formula.structure.formula.predicate.Literal;
import de.featjar.formula.structure.map.TermMap;
import de.featjar.formula.structure.formula.connective.And;
import de.featjar.formula.structure.formula.connective.Or;
import de.featjar.base.io.format.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Parses feature model formulas from FeatureIDE XML files.
 *
 * @author Sebastian Krieter
 * @author Elias Kuiter
 */
public class XMLFeatureModelFormat extends AbstractXMLFeatureModelFormat<Expression, Literal, Boolean> {
    protected final List<Expression> constraints = new ArrayList<>();
    protected final TermMap termMap = new TermMap();

    @Override
    public XMLFeatureModelFormat getInstance() {
        return new XMLFeatureModelFormat();
    }

    @Override
    public String getName() {
        return "FeatureIDE";
    }

    @Override
    public boolean supportsParse() {
        return true;
    }

    @Override
    protected Expression parseDocument(Document document) throws ParseException {
        final Element featureModelElement = getDocumentElement(document, FEATURE_MODEL);
        parseFeatureTree(getElement(featureModelElement, STRUCT));
        Optional<Element> constraintsElement = getOptionalElement(featureModelElement, CONSTRAINTS);
        if (constraintsElement.isPresent()) parseConstraints(constraintsElement.get(), termMap);
        if (constraints.isEmpty()) {
            return new And();
        } else {
            if (constraints.get(0).getChildren().isEmpty()) {
                constraints.set(0, new Or());
            }
        }
        return new And(constraints);
    }

    @Override
    protected void writeDocument(Expression object, Document doc) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Pattern getInputHeaderPattern() {
        return AbstractXMLFeatureModelFormat.inputHeaderPattern;
    }

    @Override
    protected Literal createFeatureLabel(
            String name, Literal parentFeatureLabel, boolean mandatory, boolean _abstract, boolean hidden)
            throws ParseException {
        if (termMap.hasVariable(name)) {
            throw new ParseException("Duplicate feature name!");
        } else {
            termMap.addBooleanVariable(name);
        }

        Literal literal = termMap.createLiteral(name);
        if (parentFeatureLabel == null) {
            constraints.add(literal);
        } else {
            constraints.add(implies(literal, parentFeatureLabel));
            if (mandatory) {
                constraints.add(implies(parentFeatureLabel, literal));
            }
        }
        return literal;
    }

    @Override
    protected void addAndGroup(Literal featureLabel, List<Literal> childFeatureLabels) {}

    @Override
    protected void addOrGroup(Literal featureLabel, List<Literal> childFeatureLabels) {
        constraints.add(implies(featureLabel, childFeatureLabels));
    }

    @Override
    protected void addAlternativeGroup(Literal featureLabel, List<Literal> childFeatureLabels) {
        if (childFeatureLabels.size() == 1) {
            constraints.add(implies(featureLabel, childFeatureLabels.get(0)));
        } else {
            constraints.add(new And(implies(featureLabel, childFeatureLabels), atMostOne(childFeatureLabels)));
        }
    }

    @Override
    protected void addFeatureMetadata(Literal featureLabel, Element e) {}

    @Override
    protected Boolean createConstraintLabel() {
        return true;
    }

    @Override
    protected void addConstraint(Boolean constraintLabel, Expression expression) throws ParseException {
        constraints.add(expression);
    }

    @Override
    protected void addConstraintMetadata(Boolean constraintLabel, Element e) {}
}
